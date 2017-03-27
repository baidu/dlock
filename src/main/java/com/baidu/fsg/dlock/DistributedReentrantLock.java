/*
 * Copyright (c) 2017 Baidu, Inc. All Rights Reserve.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baidu.fsg.dlock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;

import com.baidu.fsg.dlock.domain.DLockConfig;
import com.baidu.fsg.dlock.domain.DLockEntity;
import com.baidu.fsg.dlock.domain.DLockStatus;
import com.baidu.fsg.dlock.exception.DLockProcessException;
import com.baidu.fsg.dlock.exception.OptimisticLockingException;
import com.baidu.fsg.dlock.processor.DLockProcessor;
import com.baidu.fsg.dlock.utils.NetUtils;

/**
 * DistributedReentrantLock implements the lock,tryLock syntax of {@link Lock} by different mechanisms:<br>
 * <li>database</li>
 * The database synchronization primitives(line lock with conditional "UPDATE" statement).
 *
 * <li>redis</li>
 * The Atomic redis command & Lua script, guaranteed the atomic operations.<br>
 * The expire mechanisms of redis, guaranteed the lock will be released without expanding lease request,
 * so that the other competitor can try to lock.<p>
 *
 * We use a variant of CLH lock queue for the competitor threads, provides an unfair implement to make high
 * throughput.
 *
 * @author chenguoqing
 * @author yutianbao
 */
public class DistributedReentrantLock implements Lock {

    /**
     * Lock configuration
     */
    private final DLockConfig lockConfig;
    /**
     * Lock processor
     */
    private final DLockProcessor lockProcessor;

    /**
     * Head of the wait queue, lazily initialized. Except for initialization, it is modified only via method setHead.
     * Note: If head exists, its waitStatus is guaranteed not to be CANCELLED.
     */
    private final AtomicReference<Node> head = new AtomicReference<>();
    /**
     * Tail of the wait queue, lazily initialized. Modified only via method enq to add new wait node.
     */
    private final AtomicReference<Node> tail = new AtomicReference<>();

    /**
     * The current owner of exclusive mode synchronization.
     */
    private final AtomicReference<Thread> exclusiveOwnerThread = new AtomicReference<>();
    /**
     * Retry thread reference
     */
    private final AtomicReference<RetryLockThread> retryLockRef = new AtomicReference<>();
    /**
     * Expand lease thread reference
     */
    private final AtomicReference<ExpandLockLeaseThread> expandLockRef = new AtomicReference<>();

    /**
     * Once a thread hold this lock, the thread can reentrant the lock.
     * This value represents the count of holding this lock. Default as 0
     */
    private final AtomicInteger holdCount = new AtomicInteger(0);

    /**
     * CLH Queue Node for holds all parked thread
     */
    static class Node {
        final AtomicReference<Node> prev = new AtomicReference<>();
        final AtomicReference<Node> next = new AtomicReference<>();
        final Thread t;

        Node() {
            this(null);
        }

        Node(Thread t) {
            this.t = t;
        }
    }

    /**
     * Constructor with lock configuration and lock processor
     */
    public DistributedReentrantLock(DLockConfig lockConfig, DLockProcessor lockProcessor) {
        this.lockConfig = lockConfig;
        this.lockProcessor = lockProcessor;
    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void lock() {
        // lock db record
        if (!tryLock()) {
            acquireQueued(addWaiter());
        }
    }

    final void acquireQueued(final Node node) {
        for (;;) {
            final Node p = node.prev.get();
            if (p == head.get() && tryLock()) {
                head.set(node);
                p.next.set(null); // help GC
                break;
            }

            // if need, start retry thread
            if (exclusiveOwnerThread.get() == null) {
                startRetryThread();
            }

            // park current thread
            LockSupport.park(this);
        }
    }

    private Node addWaiter() {
        Node node = new Node(Thread.currentThread());
        // Try the fast path of enq; backup to full enq on failure
        Node pred = tail.get();
        if (pred != null) {
            node.prev.set(pred);
            if (tail.compareAndSet(pred, node)) {
                pred.next.set(node);
                return node;
            }
        }
        enq(node);
        return node;
    }

    private Node enq(final Node node) {
        for (;;) {
            Node t = tail.get();
            if (t == null) { // Must initialize
                Node h = new Node(); // Dummy header
                h.next.set(node);
                node.prev.set(h);
                if (head.compareAndSet(null, h)) {
                    tail.set(node);
                    return h;
                }
            } else {
                node.prev.set(t);
                if (tail.compareAndSet(t, node)) {
                    t.next.set(node);
                    return t;
                }
            }
        }
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    /**
     * Lock redis record through the atomic command Set(key, value, NX, PX, expireTime), only one request will success
     * while multiple concurrently requesting.
     */
    @Override
    public boolean tryLock() {

        // current thread can reentrant, and locked times add once
        if (Thread.currentThread() == this.exclusiveOwnerThread.get()) {
            this.holdCount.incrementAndGet();
            return true;
        }

        DLockEntity newLock = new DLockEntity();
        newLock.setLockTime(System.currentTimeMillis());
        newLock.setLocker(generateLocker());
        newLock.setLockStatus(DLockStatus.PROCESSING);

        boolean locked = false;
        try {
            // get lock directly
            lockProcessor.updateForLock(newLock, lockConfig);
            locked = true;

        } catch (OptimisticLockingException | DLockProcessException e) {
            // NOPE. Retry in the next round.
        }

        if (locked) {
            // set exclusive thread
            this.exclusiveOwnerThread.set(Thread.currentThread());

            // locked times reset to one
            this.holdCount.set(1);

            // shutdown retry thread
            shutdownRetryThread();

            // start the timer for expand lease time
            startExpandLockLeaseThread(newLock);
        }

        return locked;
    }

    /**
     * Attempts to release this lock.<p>
     *
     * If the current thread is the holder of this lock then the hold
     * count is decremented.  If the hold count is now zero then the lock
     * is released.  If the current thread is not the holder of this
     * lock then {@link IllegalMonitorStateException} is thrown.
     *
     * @throws IllegalMonitorStateException if the current thread does not
     *         hold this lock
     */
    @Override
    public void unlock() throws IllegalMonitorStateException {
        // lock must be hold by current thread
        if (Thread.currentThread() != this.exclusiveOwnerThread.get()) {
            throw new IllegalMonitorStateException();
        }

        // lock is still be hold
        if (holdCount.decrementAndGet() > 0) {
            return;
        }

        // clear remote lock
        DLockEntity currentLock = new DLockEntity();
        currentLock.setLocker(generateLocker());
        currentLock.setLockStatus(DLockStatus.PROCESSING);

        try {
            // release remote lock
            lockProcessor.updateForUnlock(currentLock, lockConfig);

        } catch (OptimisticLockingException | DLockProcessException e) {
            // NOPE. Lock will deleted automatic after the expire time.

        } finally {
            // Release exclusive owner
            this.exclusiveOwnerThread.compareAndSet(Thread.currentThread(), null);

            // Shutdown expand thread
            shutdownExpandThread();

            // wake up the head node for compete lock
            unparkQueuedNode();
        }
    }

    /**
     * wake up the head node for compete lock
     */
    private void unparkQueuedNode() {
        // wake up the head node for compete lock
        Node h = head.get();
        if (h != null && h.next.get() != null) {
            LockSupport.unpark(h.next.get().t);
        }
    }

    /**
     * Generate current locker. IP_Thread ID
     */
    private String generateLocker() {
        return NetUtils.getLocalAddress() + "-" + Thread.currentThread().getId();
    }

    /**
     * Task for expanding the lock lease
     */
    abstract class LockThread extends Thread {
        /**
         * Synchronizes
         */
        final Object sync = new Object();
        /**
         * Delay time for start(ms)
         */
        final int delay;
        /**
         * Retry interval(ms)
         */
        final int retryInterval;

        final AtomicInteger startState = new AtomicInteger(0);
        /**
         * Control variable for shutdown
         */
        private boolean shouldShutdown = false;
        /**
         * Is first running
         */
        private boolean firstRunning = true;

        LockThread(String name, int delay, int retryInterval) {
            setDaemon(true);
            this.delay = delay;
            this.retryInterval = retryInterval;
            setName(name + "-" + getId());
        }

        @Override
        public void run() {
            while (!shouldShutdown) {
                synchronized (sync) {
                    try {
                        // first running, delay
                        if (firstRunning && delay > 0) {
                            firstRunning = false;
                            sync.wait(delay);
                        }

                        // execute task
                        execute();

                        // wait for interval
                        sync.wait(retryInterval);

                    } catch (InterruptedException e) {
                        shouldShutdown = true;
                    }
                }
            }

            // clear associated resources for implementations
            beforeShutdown();
        }

        abstract void execute() throws InterruptedException;

        void beforeShutdown() {
        }
    }

    /**
     * Task for expanding the lock lease
     */
    private class ExpandLockLeaseThread extends LockThread {

        final DLockEntity lock;

        ExpandLockLeaseThread(DLockEntity lock, int delay, int retryInterval) {
            super("ExpandLockLeaseThread", delay, retryInterval);
            this.lock = lock;
        }

        @Override
        void execute() throws InterruptedException {
            try {
                // set lock time
                lock.setLockTime(System.currentTimeMillis());

                // update lock
                lockProcessor.expandLockExpire(lock, lockConfig);

            } catch (OptimisticLockingException e) {
                // if lock has been released, kill current thread
                throw new InterruptedException("Lock released.");

            } catch (DLockProcessException e) {
                // retry
            }
        }

        @Override
        void beforeShutdown() {
            expandLockRef.compareAndSet(this, null);
        }
    }

    private void startExpandLockLeaseThread(DLockEntity lock) {
        ExpandLockLeaseThread t = expandLockRef.get();

        while (t == null || t.getState() == Thread.State.TERMINATED) {
            // set new expand lock thread
            int retryInterval = (int) (lockConfig.getMillisLease() * 0.75);
            expandLockRef.compareAndSet(t, new ExpandLockLeaseThread(lock, 1, retryInterval));

            // retrieve the new expand thread instance
            t = expandLockRef.get();
        }

        if (t.startState.compareAndSet(0, 1)) {
            t.start();
        }
    }

    private void shutdownExpandThread() {
        ExpandLockLeaseThread t = expandLockRef.get();
        if (t != null && t.isAlive()) {
            t.interrupt();
        }
    }

    /**
     * Start when: (1) no threads hold lock; (2) CLH has waiting thread(s). And shutdown when one thread
     * posses the lock, because it does not has necessary to start retry thread.
     */
    private class RetryLockThread extends LockThread {

        RetryLockThread(int delay, int retryInterval) {
            super("RetryLockThread", delay, retryInterval);
        }

        @Override
        void execute() throws InterruptedException {

            // if existing running thread, kill self
            if (exclusiveOwnerThread.get() != null) {
                throw new InterruptedException("Has running thread.");
            }

            Node h = head.get();

            // no thread for lock, kill self
            if (h == null) {
                throw new InterruptedException("No waiting thread.");
            }

            boolean needRetry = false;
            try {
                needRetry = lockProcessor.isLockFree(lockConfig.getLockUniqueKey());
            } catch (DLockProcessException e) {
                needRetry = true;
            }

            // if the lock has been releases or expired, re-competition  
            if (needRetry) {
                // wake up the head node for compete lock
                unparkQueuedNode();
            }
        }

        @Override
        void beforeShutdown() {
            retryLockRef.compareAndSet(this, null);
        }
    }

    /**
     * Start the retry thread
     */
    private void startRetryThread() {
        RetryLockThread t = retryLockRef.get();

        while (t == null || t.getState() == Thread.State.TERMINATED) {
            retryLockRef.compareAndSet(t, new RetryLockThread((int) (lockConfig.getMillisLease() / 10),
                    (int) (lockConfig.getMillisLease() / 6)));

            t = retryLockRef.get();
        }

        if (t.startState.compareAndSet(0, 1)) {
            t.start();
        }
    }

    /**
     * Shutdown retry thread
     */
    private void shutdownRetryThread() {
        RetryLockThread t = retryLockRef.get();
        if (t != null && t.isAlive()) {
            t.interrupt();
        }
    }
}

package com.baidu.fsg.dlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;

import javax.annotation.Resource;

import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.baidu.fsg.dlock.domain.DLockConfig;
import com.baidu.fsg.dlock.jedis.JedisClient;
import com.baidu.fsg.dlock.processor.impl.RedisLockProcessor;
import com.baidu.fsg.dlock.utils.ReflectionUtils;

/**
 * Test for {@link DistributedReentrantLock}.<p>
 * 
 * <B>Note:</B>No redis mock is provided, to be continue for mock test.<br>
 * If you can't connect to BDRP, try to connect VPN first. 
 * 
 * @author yutianbao
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:dlock/spring-dlock.xml" })
public class DistributedReentrantLockTest {

    @Resource
    private RedisLockProcessor lockProcessor;

    @Resource
    private JedisClient jedisClient;

    /**
     * DistributedReentrantLock instance
     */
    private Lock lockOnServer1;

    /**
     * DistributedReentrantLock. Simulate as another server
     */
    private Lock lockOnServer2;

    /**
     * DistributedReentrantLock. Used for single server test
     */
    private Lock lockSingleServer;

    /**
     * CountDownLatch used for multi servers
     */
    private static CountDownLatch cdLatch = new CountDownLatch(2);

    @Before
    public void setup() {
        DLockConfig singleServerLockConfig = new DLockConfig("USER_LOCK", "778899", 1000, TimeUnit.MILLISECONDS);
        lockSingleServer = new DistributedReentrantLock(singleServerLockConfig, lockProcessor);

        // The retry thread's execute interval is depended on The lease duration (Retry interval = lease ms * 0.75)
        DLockConfig multiServerLockConfig = new DLockConfig("USER_LOCK", "778899", 500, TimeUnit.MILLISECONDS);
        lockOnServer1 = new DistributedReentrantLock(multiServerLockConfig, lockProcessor);
        lockOnServer2 = new DistributedReentrantLock(multiServerLockConfig, lockProcessor);

        // Delete unique key of the last round test
        jedisClient.del(singleServerLockConfig.getLockUniqueKey());
        jedisClient.del(multiServerLockConfig.getLockUniqueKey());
    }

    /**
     * Case1: Test for reentrant feature
     */
     @Test
    public void testReentrant() throws Exception {
        // Reentrant caller
        reentrantGateOne(lockSingleServer);

        // Assert no holder for this lock
        checkHoldCnt(lockSingleServer);
    }

    /**
     * Case2: Test for one server - multi threads
     */
    @Test
    public void testSingleServer() throws Exception {
        // Thread max work elapse is 2s, greater than the lease duration (1s)
        // It means when the thread hold the lock, lease must be expanded.
        launchSingleServer(50, "S1", lockSingleServer, 2000);

        // joinThreads(threads);
        checkHoldCnt(lockSingleServer);
    }

    /**
     * Case3: Test for multi servers - multi threads
     */
     @Test
    public void testMultiServer() throws InterruptedException {

        try {
            // Launch servers "S1", "S2"
            new ServerThread(50, "S1", lockOnServer1, 2000).start();
            new ServerThread(50, "S2", lockOnServer2, 2000).start();

            // Check
            cdLatch.await();

            checkHoldCnt(lockOnServer1);
            checkHoldCnt(lockOnServer2);

        } catch (Exception e) {
            Assert.fail();
        }
    }

    /**
     * Launch threads on a single server
     * 
     * @param totalThread
     * @param serverName
     * @param lock
     * @param maxWorkElapsed
     */
    private static void launchSingleServer(int totalThread, String serverName, Lock lock, int maxWorkElapsed) {
        List<Thread> threads = new ArrayList<>(totalThread);
        for (int i = 0; i < totalThread; i++) {
            String tName = serverName + "-t" + StringUtils.leftPad(i + "", 2, "0");
            threads.add(i, new RedisTestThread(tName, lock, maxWorkElapsed));
        }

        threads.forEach(t -> t.start());
        threads.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
            }
        });

        System.out.println("**** All Done **** " + serverName);
    }

    /**
     * Check hold cnt of lock
     */
    private void checkHoldCnt(Lock lock) throws Exception {
        AtomicInteger holdCnt = (AtomicInteger) ReflectionUtils.getProperty(lock, "holdCount");
        Assert.assertEquals(0, holdCnt.get());
    }

    /**
     * Server thread
     */
    static class ServerThread extends Thread {
        int totalThread;
        String serverName;
        Lock lock;
        int maxWorkElapsed;

        ServerThread(int totalThread, String serverName, Lock lock, int maxWorkElapsed) {
            super(serverName);
            this.totalThread = totalThread;
            this.serverName = serverName;
            this.lock = lock;
            this.maxWorkElapsed = maxWorkElapsed;
            setDaemon(true);
        }

        @Override
        public void run() {
            launchSingleServer(totalThread, serverName, lock, maxWorkElapsed);
            cdLatch.countDown();
        }

    }

    /**
     * Test thread
     */
    static class RedisTestThread extends Thread {

        private Lock redisLock;
        private int maxWorkElapse;

        RedisTestThread(String name, Lock redisLock, int maxWorkElapse) {
            super(name);
            this.redisLock = redisLock;
            this.maxWorkElapse = maxWorkElapse;
            setDaemon(true);
        }

        @Override
        public void run() {
            long start = System.currentTimeMillis();

            while (System.currentTimeMillis() - start <= 60 * 1000L) {

                try {
                    long t = System.currentTimeMillis();
                    redisLock.lock();
                    System.out.println("*********************** Lock block ***********************");
                    System.out.println(getName() + " >>>>> get lock time:" + (System.currentTimeMillis() - t));
                    
                    doWork();
                    
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    redisLock.unlock();
                }

                break;
            }
            System.out.println(getName() + " >     Done!");
            System.out.println();
        }

        private void doWork() throws InterruptedException {
            int sleepTime = new Random().nextInt(maxWorkElapse);
            sleep(sleepTime);
            System.out.println(getName() + " >>>   worked done for:" + sleepTime);
        }
    }

    /**
     * Reentrant test tool methods
     */
    private void reentrantGateOne(Lock lock) {
        try {
            System.out.println("method1 ready to lock.");
            lock.lock();
            System.out.println("method1 lock success. ++");

            reentrantGateTwo(lock);
            Thread.sleep(1000);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("method1 ready to release lock.");
            lock.unlock();
            System.out.println("method1 unlocked success. --");
        }
    }

    private void reentrantGateTwo(Lock lock) {
        try {
            System.out.println(">>>>method2 ready to lock.");
            lock.lock();
            System.out.println(">>>>method2 lock success. ++");

            Thread.sleep(1500);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println(">>>>method2 ready to release lock.");
            lock.unlock();
            System.out.println(">>>>method2 unlocked success. --");
        }
    }

}

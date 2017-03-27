package com.baidu.fsg.dlock;

import com.baidu.fsg.dlock.support.DLockGenerator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * Test for {@link DLockGenerator}, simple use of lock() & tryLock()
 *
 * @author yutianbao
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:dlock/spring-dlock.xml"})
public class DLockSimpleTest {

    @Resource
    private DLockGenerator lockGenerator;

    /**
     * Case1: Test for lock()
     */
    @Test
    public void testLock() {
        // generate lock in a free way, you should specify lock type, target, lease time, lease unit
        Lock lock = lockGenerator.gen("FAKE_LOCK", "A_TARGET", 1, TimeUnit.SECONDS);

        try {
            lock.lock();
            doYourWork();

        } finally {
            // Make sure unlock in the finally code block
            lock.unlock();
        }
    }

    /**
     * Case2: Test for tryLock()
     */
    @Test
    public void testTryLock() {
        // generate lock in a free way, you should specify lock type, target, lease time, lease unit
        Lock lock = lockGenerator.gen("FAKE_LOCK", "A_TARGET", 1, TimeUnit.SECONDS);

        if (lock.tryLock()) {
            try {
                doYourWork();
            } finally {
                lock.unlock();
            }
        } else {
            // perform alternative actions
        }
    }

    /**
     * Do your work
     */
    private void doYourWork() {
        System.out.println("Just do your work");
    }

}

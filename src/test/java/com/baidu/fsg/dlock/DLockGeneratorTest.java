package com.baidu.fsg.dlock;

import com.baidu.fsg.dlock.domain.DLockConfig;
import com.baidu.fsg.dlock.domain.DLockType;
import com.baidu.fsg.dlock.support.DLockGenerator;
import com.baidu.fsg.dlock.utils.ReflectionUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * Test for {@link DLockGenerator}
 * 
 * @author yutianbao
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:dlock/spring-dlock.xml" })
public class DLockGeneratorTest {

    @Resource
    private DLockGenerator lockGenerator;

    @Test
    public void testLockGenerate() throws Exception {
        Assert.assertNotNull(lockGenerator);

        // generate lock with a default lease time
        Lock lock = lockGenerator.gen(DLockType.CUSTOMER_LOCK, "12345");
        checkLock(lock, lockGenerator.getLockConfigMap().get(DLockType.CUSTOMER_LOCK).intValue());

        // generate lock with a specified lease time
        lock = lockGenerator.gen(DLockType.CUSTOMER_LOCK, "12345", 500);
        checkLock(lock, 500);

        // generate lock in a free way, you must specify lock type, target, lease time, lease unit
        lock = lockGenerator.gen("FAKE_LOCK", "A_TARGET", 1, TimeUnit.SECONDS);
        checkLock(lock, 1);

    }

    /**
     * Check lock
     */
    private void checkLock(Lock lock, int expectedLeaseTime) throws Exception {
        DLockConfig lockConfig = (DLockConfig) ReflectionUtils.getProperty(lock, "lockConfig");
        Assert.assertEquals(expectedLeaseTime, lockConfig.getLease());
    }

}

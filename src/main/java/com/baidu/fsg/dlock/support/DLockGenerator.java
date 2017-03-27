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
package com.baidu.fsg.dlock.support;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import com.baidu.fsg.dlock.DistributedReentrantLock;
import com.baidu.fsg.dlock.domain.DLockConfig;
import com.baidu.fsg.dlock.domain.DLockType;
import com.baidu.fsg.dlock.processor.DLockProcessor;
import com.baidu.fsg.dlock.utils.EnumUtils;

/**
 * DLockGenerator represents a generator for {@link DistributedReentrantLock} <br>
 * It will load the lease time of {@link DLockType} configured in file config-dlock.properties, key is DLockType Enum
 * name, value is lease time(ms). Sample as below:<p>
 * 
 * <code>CUSTOMER_LOCK=1000</code><br>
 * <code>XXXXXXXX_LOCK=2000</code>
 * 
 * @author yutianbao
 */
@Service
public class DLockGenerator {

    /** Default dlock configuration path */
    private static final String DEFAULT_CONF_PATH = "dlock/config-dlock.properties";

    @Resource
    private DLockProcessor lockProcessor;

    /**
     * Key for DLockType enum, Value for lease time (ms)
     */
    private Map<DLockType, Integer> lockConfigMap = new HashMap<>();

    /**
     * dlock properties configuration file path
     */
    private String confPath;

    /**
     * Load the lease config from properties, and init the lockConfigMap.
     */
    @PostConstruct
    public void init() {
        try {
            // Using default path if no specified
            confPath = StringUtils.isBlank(confPath) ? DEFAULT_CONF_PATH : confPath;

            // Load lock config from properties
            Properties properties = PropertiesLoaderUtils.loadAllProperties(confPath);

            // Build the lockConfigMap
            for (Entry<Object, Object> propEntry : properties.entrySet()) {
                DLockType lockType = EnumUtils.valueOf(DLockType.class, propEntry.getKey().toString());
                Integer lease = Integer.valueOf(propEntry.getValue().toString());

                if (lockType != null && lease != null) {
                    lockConfigMap.put(lockType, lease);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Load distributed lock config fail", e);
        }
    }

    /**
     * Get lock with a default lease time configured in the config-dlock.properties
     *
     * @param lockType enum DLockType
     * @param lockTarget
     * @return
     */
    public Lock gen(DLockType lockType, String lockTarget) {
        // pre-check
        Integer lease = lockConfigMap.get(lockType);
        Assert.notNull(lease, "unfound config for DLockType:" + lockType);

        return getLockInstance(lockType.name(), lockTarget, lease, TimeUnit.MILLISECONDS);
    }

    /**
     * Get lock with a specified lease time
     *
     * @param lockType enum DLockType
     * @param lockTarget
     * @param leaseTime
     * @return
     */
    public Lock gen(DLockType lockType, String lockTarget, int leaseTime) {
        // pre-check
        Assert.notNull(lockType, "lockType can't be null!");
        Assert.isTrue(leaseTime > 0, "leaseTime must greater than zero!");

        return getLockInstance(lockType.name(), lockTarget, leaseTime, TimeUnit.MILLISECONDS);
    }

    /**
     * A free way to make the instance of {@link DistributedReentrantLock}
     *
     * @param lockTypeStr
     * @param lockTarget
     * @param lease
     * @param leaseTimeUnit
     * @return
     */
    public Lock gen(String lockTypeStr, String lockTarget, int lease, TimeUnit leaseTimeUnit) {
        // pre-check
        Assert.isTrue(StringUtils.isNotEmpty(lockTypeStr), "lockTypeStr can't be empty!");
        Assert.isTrue(lease > 0, "leaseTime must greater than zero!");
        Assert.notNull(leaseTimeUnit, "leaseTimeUnit can't be null!");

        return getLockInstance(lockTypeStr, lockTarget, lease, leaseTimeUnit);
    }

    /**
     * Get lockConfigMap(unmodifiableMap)
     */
    @SuppressWarnings("unchecked")
    public Map<DLockType, Integer> getLockConfigMap() {
        return MapUtils.unmodifiableMap(lockConfigMap);
    }

    /**
     * Generate instance of DistributedReentrantLock
     */
    private Lock getLockInstance(String lockTypeStr, String lockTarget, int lease, TimeUnit leaseTimeUnit) {
        DLockConfig dlockConfig = new DLockConfig(lockTypeStr, lockTarget, lease, leaseTimeUnit);
        return new DistributedReentrantLock(dlockConfig, lockProcessor);
    }

    /**
     * Setter for spring field
     */
    public void setConfPath(String confPath) {
        this.confPath = confPath;
    }
}

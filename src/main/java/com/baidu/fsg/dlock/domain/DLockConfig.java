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
package com.baidu.fsg.dlock.domain;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * This class representing a distribute lock configuration.<br>
 * The minimum granularity of the lock entity is LockUniqueKey, which consists of $UK_PRE_$LockType_$LockTarget, 
 * You can set a specified lease time for each lockUniqueKey<p>
 * 
 * Sample:<br>
 * LockType: USER_LOCK, LockTartget: 2356784, Lease: 500<br>
 * LockType: USER_LOCK, LockTartget: 2356783, Lease: 500<p>
 *
 * LockType: BATCH_PROCESS_LOCK, LockTartget: MAP_NODE, Lease: 300<br>
 * LockType: BATCH_PROCESS_LOCK, LockTartget: REDUCE_NODE, Lease: 400
 * 
 * @author yutianbao
 */
public class DLockConfig implements Serializable {
    private static final long serialVersionUID = -1332663877601479136L;

    /** Prefix for unique key generating */
    public static final String UK_PRE = "DLOCK";

    /** Separator for unique key generating */
    public static final String UK_SP = "_";

    /**
     * Lock type represents a group lockTargets with the same type.  
     * The type is divided by different business scenarios, kind of USER_LOCK, ORDER_LOCK, BATCH_PROCCESS_LOCK...
     */
    private final String lockType;

    /**
     * Lock target represents a real lock target. lockType: USER_LOCK, lockTarget should be the UserID. 
     */
    private final String lockTarget;

    /**
     * Lock unique key represents the minimum granularity of the lock. 
     * The naming policy is $UK_PRE_$lockType_$lockTarget
     */
    private final String lockUniqueKey;

    /**
     * Lock lease duration
     */
    private final int lease;

    /**
     * Lock Lease time unit
     */
    private final TimeUnit leaseTimeUnit;

    /**
     * Constructor with lockType & lockTarget & leaseTime & leaseTimeUnit
     */
    public DLockConfig(String lockType, String lockTarget, int lease, TimeUnit leaseTimeUnit) {
        this.lockType = lockType;
        this.lockTarget = lockTarget;
        this.lockUniqueKey = UK_PRE + UK_SP + lockType + UK_SP + StringUtils.trimToEmpty(lockTarget);
        this.lease = lease;
        this.leaseTimeUnit = leaseTimeUnit;
    }

    /**
     * Getters
     */
    public String getLockType() {
        return lockType;
    }

    public String getLockTarget() {
        return lockTarget;
    }

    public int getLease() {
        return lease;
    }

    public TimeUnit getLeaseTimeUnit() {
        return leaseTimeUnit;
    }

    public String getLockUniqueKey() {
        return lockUniqueKey;
    }
    
    /**
     * Get the lease of millis unit
     */
    public long getMillisLease() {
        return leaseTimeUnit.toMillis(lease);
    }
    
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.DEFAULT_STYLE);
    }

}

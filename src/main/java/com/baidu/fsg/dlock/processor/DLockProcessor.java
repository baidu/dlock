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
package com.baidu.fsg.dlock.processor;

import com.baidu.fsg.dlock.domain.DLockConfig;
import com.baidu.fsg.dlock.domain.DLockEntity;

/**
 * The distributed lock processor interface for retrieving and updating lock status
 * to persistent system(such as Redis/DB).
 *
 * @author chenguoqing
 */
public interface DLockProcessor {

    /**
     * Retrieve the {@link DLockEntity} by the unique key
     *
     * @param uniqueKey
     * @return
     */
    DLockEntity load(String uniqueKey);

    /**
     * The method implements the "lock" syntax<br>
     * <li>DB</li>
     * The implementations should update the (lockStatus,locker,lockTime) with
     * DB record lock under the condition (lockStatus=0)<p>
     *
     * <li>Redis</li>
     * The implementations should set unique key, value(locker), and expire time
     *
     * @param newLock
     * @param lockConfig
     * @throw OptimisticLockingFailureException
     */
    void updateForLock(DLockEntity newLock, DLockConfig lockConfig);

    /**
     * The method implements the "lock" syntax with existing expire lock.<br>
     * <li>DB</li>
     * The implementations should update
     * (lockStatus,locker,lockTime) with DB line lock under the condition (lockStatus=1 && locker==expireLock.locker)<p>
     *
     * <li>Redis</li>
     * The implementation is unsupported because of the Redis expire mechanism.
     *
     * @param expireLock
     * @param dbLock
     * @param lockConfig
     */
    void updateForLockWithExpire(DLockEntity expireLock, DLockEntity dbLock, DLockConfig lockConfig);

    /**
     * Expand the lock expire time. It should be protected with DB line lock, it only modify the lockTime field.
     *
     * @param newLeaseLock
     * @param lockConfig
     */
    void expandLockExpire(DLockEntity newLeaseLock, DLockConfig lockConfig);

    /**
     * The method implements the "unlock" syntax.<br>
     *
     * <li>DB</li>
     * The implementations should should reset the lock status to INITIAL, and clear locker,
     * lockTime fields with optimistic lock condition(lockStatus,locker). The operation should be protected with
     * DB line lock.<br><br>
     *
     * <li>Redis</li>
     * The implementation should remove key with the right value(locker).
     */
    void updateForUnlock(DLockEntity currentLock, DLockConfig lockConfig);

    /**
     * Whether the lock is free(released or expired)
     *
     * @param uniqueKey key
     * @return true if lock is released
     */
    boolean isLockFree(String uniqueKey);

}

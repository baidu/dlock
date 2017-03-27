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

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * DLockEntity represents an distributed lock entity, consists of  lock status, locker, lockTime.
 * 
 * @author chenguoqing
 * @author yutianbao
 */
public class DLockEntity implements Serializable, Cloneable {
    private static final long serialVersionUID = 8479390959137749786L;

    /**
     * Task status default as {@link DLockStatus#INITIAL}
     */
    private DLockStatus lockStatus = DLockStatus.INITIAL;

    /**
     * The server ip address that locked the task
     */
    private String locker;

    /**
     * Lock time for milliseconds
     */
    private Long lockTime = -1L;

    /**
     * Constructor
     */
    public DLockEntity() {
    }

    /**
     * Getters & Setters
     */
    public DLockStatus getLockStatus() {
        return lockStatus;
    }

    public void setLockStatus(DLockStatus lockStatus) {
        this.lockStatus = lockStatus;
    }

    public String getLocker() {
        return locker;
    }

    public void setLocker(String locker) {
        this.locker = locker;
    }

    public Long getLockTime() {
        return lockTime;
    }

    public void setLockTime(Long lockTime) {
        this.lockTime = lockTime;
    }
    
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.DEFAULT_STYLE);
    }

}

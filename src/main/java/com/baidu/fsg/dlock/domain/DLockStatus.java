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

import com.baidu.fsg.dlock.utils.ValuedEnum;

/**
 * Lock status
 *
 * @author chenguoqing
 */
public enum DLockStatus implements ValuedEnum<Integer> {

    INITIAL(0),
    PROCESSING(1);

    /**
     * Lock status
     */
    private final int status;

    /**
     * Constructor with field of status
     */
    DLockStatus(int status) {
        this.status = status;
    }

    @Override
    public Integer value() {
        return status;
    }
}

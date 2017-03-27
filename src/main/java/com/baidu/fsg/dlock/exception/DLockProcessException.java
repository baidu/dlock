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
package com.baidu.fsg.dlock.exception;

/**
 * DLockProcessException
 * 
 * @author yutianbao
 */
public class DLockProcessException extends RuntimeException {

    /**
     * Serial Version UID
     */
    private static final long serialVersionUID = -4147467240172878091L;

    /**
     * Default constructor
     */
    public DLockProcessException() {
        super();
    }

    /**
     * Constructor with message & cause
     * @param message
     * @param cause
     */
    public DLockProcessException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor with message
     * @param message
     */
    public DLockProcessException(String message) {
        super(message);
    }

    /**
     * Constructor with cause
     * @param cause
     */
    public DLockProcessException(Throwable cause) {
        super(cause);
    }

}

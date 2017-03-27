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
package com.baidu.fsg.dlock.jedis;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Service;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Jedis client
 *
 * @author yutianbao
 */
@Service
public class JedisClient {

    @Resource
    private JedisPool jedisPool;

    /**
     * String get command
     *
     * @param key
     * @return
     */
    public String get(String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            return jedis.get(key);

        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    /**
     * String set command
     *
     * @param key
     * @param value
     * @param nxxx
     * @param expx
     * @param time
     * @return
     */
    public String set(String key, String value, String nxxx, String expx, long time) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            return jedis.set(key, value, nxxx, expx, time);

        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    /**
     * Eval lua script command
     *
     * @param script
     * @param keys
     * @param args
     * @return
     */
    public Object eval(String script, List<String> keys, List<String> args) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            return jedis.eval(script, keys, args);

        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    /**
     * String delete command
     *
     * @param key
     * @return
     */
    public Long del(String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            return jedis.del(key);

        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

}

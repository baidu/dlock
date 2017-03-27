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
package com.baidu.fsg.dlock.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;

/**
 * ReflectionUtils
 *
 * @author yutianbao
 */
public abstract class ReflectionUtils {

    /**
     * Void
     */
    public static boolean isVoid(Class<?> clazz) {
        return clazz == void.class || clazz == Void.class;
    }

    /**
     * Property
     */
    public static Object getProperty(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    public static void setProperty(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    /**
     * Field
     */
    public static List<Field> getAllFields(Class<?> clazz) throws Exception {
        return getAllFields(clazz, null, true);
    }

    public static List<Field> getAllFields(Class<?> clazz, boolean excludeStaticField) throws Exception {
        return getAllFields(clazz, null, excludeStaticField);
    }

    public static List<Field> getAllFields(Class<?> clazz, Class<? extends Annotation> annotation) throws Exception {
        return getAllFields(clazz, annotation, true);
    }

    public static List<Field> getAllFields(Class<?> clazz, Class<? extends Annotation> annotation,
            boolean excludeStaticField) throws Exception {
        // Precondition checking
        if (clazz == null) {
            return null;
        }

        List<Field> r = new LinkedList<Field>();
        Class<?> parent = clazz;
        while (parent != null) {
            for (Field f : parent.getDeclaredFields()) {
                f.setAccessible(true);

                if (excludeStaticField && (f.getModifiers() & Modifier.STATIC) != 0) {
                    continue;
                }
                if (annotation != null && !f.isAnnotationPresent(annotation)) {
                    continue;
                }

                r.add(f);
            }

            parent = parent.getSuperclass();
        }
        return r;
    }

}

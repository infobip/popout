/*
 * Copyright 2018 Infobip Ltd.
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

package org.infobip.lib.popout.util;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

/**
 * Different reflection helpers.
 *
 * @since 1.0.0
 * @author Artem Labazin
 */
public final class ReflectionUtils {

    /**
     * Extracts field value from object by its name.
     *
     * @param obj  object, from which need to extract field's value.
     * @param name field's name
     *
     * @return optional - field's value or nothing.
     */
    @SneakyThrows
    public static Optional<Object> getFieldValueFrom (@NonNull Object obj, @NonNull String name) {
        val optional = findIn(obj, Class::getDeclaredFields, field -> field.getName().equals(name));
        if (optional.isPresent()) {
            val field = optional.get();
            AccessController.doPrivileged(new PrivilegedAction<Object>() {

                @Override
                public Object run () {
                    field.setAccessible(true);
                    return null;
                }
            });
            val value = field.get(obj);
            return ofNullable(value);
        }
        return empty();
    }

    /**
     * Invoke method of object with specified arguments.
     *
     * @param obj       object
     * @param name      method's name
     * @param arguments optional arguments for method
     *
     * @return method's invocation result.
     */
    @SneakyThrows
    public static Object invokeMethodOf (@NonNull Object obj, @NonNull String name, Object... arguments) {
        val optional = findIn(obj, Class::getDeclaredMethods, method -> method.getName().equals(name));
        if (optional.isPresent()) {
            val method = optional.get();
            return method.invoke(obj, arguments);
        }
        return null;
    }

    // Searches method/field in object by predicate.
    private static <T> Optional<T> findIn (Object obj, Function<Class<?>, T[]> extractor, Predicate<T> predicate) {
        Class<?> current = obj.getClass();

        while (current != null) {
            val values = extractor.apply(current);
            val optional = Stream.of(values)
                    .filter(predicate)
                    .findFirst();

            if (optional.isPresent()) {
                return optional;
            }
            current = current.getSuperclass();
        }
        return empty();
    }

    private ReflectionUtils () {
    }
}

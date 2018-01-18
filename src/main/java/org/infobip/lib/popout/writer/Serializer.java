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

package org.infobip.lib.popout.writer;

/**
 * A serializer interface that transforms object to a byte array.
 *
 * @since 1.0.0
 * @author Artem Labazin
 * @param <T> the argument's type of method {@code serialize}
 */
public interface Serializer<T> {

    /**
     * Serializes an object to byte array, or throw an exception if unable to do so.
     *
     * @param object object for serialization
     *
     * @return serialized result
     *
     * @throws Exception in case of serialization error
     */
    byte[] serialize (T object) throws Exception;
}

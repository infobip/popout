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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import lombok.Cleanup;
import lombok.val;

/**
 * This class implements a serializer based on default {@link ObjectOutputStream}.
 *
 * @see Serializer
 * @since 1.0.0
 * @author Artem Labazin
 * @param <T> the argument's type of method {@code serialize}
 */
public class DefaultSerializer<T> implements Serializer<T> {

    @Override
    public byte[] serialize (T object) throws IOException {
        @Cleanup
        val byteArrayOutputStream = new ByteArrayOutputStream();
        @Cleanup
        val objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);

        objectOutputStream.writeObject(object);

        return byteArrayOutputStream.toByteArray();
    }
}

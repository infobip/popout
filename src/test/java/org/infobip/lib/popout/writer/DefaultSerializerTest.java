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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.Test;

/**
 *
 * @author Artem Labazin
 */
public class DefaultSerializerTest {

    @Test
    public void serialize () throws Exception {
        String string = "Hello world";

        Serializer<String> serializer = new DefaultSerializer<>();

        byte[] bytes = serializer.serialize(string);
        assertThat(bytes).isNotNull();

        assertThat(deserialize(bytes)).isEqualTo(string);
    }

    @SneakyThrows
    private Object deserialize (byte[] bytes) {
        @Cleanup
        val byteArrayInputStream = new ByteArrayInputStream(bytes);
        @Cleanup
        val objectInputStream = new ObjectInputStream(byteArrayInputStream);
        return objectInputStream.readObject();
    }
}

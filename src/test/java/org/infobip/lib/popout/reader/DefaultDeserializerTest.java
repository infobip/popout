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

package org.infobip.lib.popout.reader;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;

/**
 *
 * @author Artem Labazin
 */
@DisplayName("Default deserializer tests")
class DefaultDeserializerTest {

  @Test
  @DisplayName("success deserialization")
  void deserialize () {
    String string = "Hello world";
    byte[] bytes = serialize(string);

    Deserializer<String> deserializer = new DefaultDeserializer<>();
    assertThat(deserializer.deserialize(bytes))
        .isNotNull()
        .isNotBlank()
        .isEqualTo(string);
  }

  @SneakyThrows
  private byte[] serialize (Object object) {
    @Cleanup
    val byteArrayOutputStream = new ByteArrayOutputStream();
    @Cleanup
    val objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);

    objectOutputStream.writeObject(object);

    return byteArrayOutputStream.toByteArray();
  }
}

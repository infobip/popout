/*
 * Copyright 2019 Infobip Ltd.
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

package org.infobip.lib.popout;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.ThreadLocalRandom;

import io.appulse.utils.Bytes;
import lombok.val;
import org.junit.jupiter.api.Test;

class SerializationTests {

  @Test
  void byteSerialization () {
    val value = (byte) 57;
    val buffer = Bytes.resizableArray();

    Serializer.BYTE.serialize(value, buffer);
    val length = buffer.writerIndex();
    val deserialized = Deserializer.BYTE.deserialize(length, buffer);

    assertThat(deserialized).isEqualTo(value);
  }

  @Test
  void shortSerialization () {
    val value = (short) 27_568;
    val buffer = Bytes.resizableArray();

    Serializer.SHORT.serialize(value, buffer);
    val length = buffer.writerIndex();
    val deserialized = Deserializer.SHORT.deserialize(length, buffer);

    assertThat(deserialized).isEqualTo(value);
  }

  @Test
  void characterSerialization () {
    val value = (char) '!';
    val buffer = Bytes.resizableArray();

    Serializer.CHARACTER.serialize(value, buffer);
    val length = buffer.writerIndex();
    val deserialized = Deserializer.CHARACTER.deserialize(length, buffer);

    assertThat(deserialized).isEqualTo(value);
  }

  @Test
  void integerSerialization () {
    val value = (int) 417_339;
    val buffer = Bytes.resizableArray();

    Serializer.INTEGER.serialize(value, buffer);
    val length = buffer.writerIndex();
    val deserialized = Deserializer.INTEGER.deserialize(length, buffer);

    assertThat(deserialized).isEqualTo(value);
  }

  @Test
  void longSerialization () {
    val value = (long) 5_040_231_345L;
    val buffer = Bytes.resizableArray();

    Serializer.LONG.serialize(value, buffer);
    val length = buffer.writerIndex();
    val deserialized = Deserializer.LONG.deserialize(length, buffer);

    assertThat(deserialized).isEqualTo(value);
  }

  @Test
  void floatSerialization () {
    val value = (float) -5.78F;
    val buffer = Bytes.resizableArray();

    Serializer.FLOAT.serialize(value, buffer);
    val length = buffer.writerIndex();
    val deserialized = Deserializer.FLOAT.deserialize(length, buffer);

    assertThat(deserialized).isEqualTo(value);
  }

  @Test
  void doubleSerialization () {
    val value = (double) 2_451.892;
    val buffer = Bytes.resizableArray();

    Serializer.DOUBLE.serialize(value, buffer);
    val length = buffer.writerIndex();
    val deserialized = Deserializer.DOUBLE.deserialize(length, buffer);

    assertThat(deserialized).isEqualTo(value);
  }

  @Test
  void bigIntegerSerialization () {
    val value = new BigInteger("5766891293459779341034571457");
    val buffer = Bytes.resizableArray();

    Serializer.BIG_INTEGER.serialize(value, buffer);
    val length = buffer.writerIndex();
    val deserialized = Deserializer.BIG_INTEGER.deserialize(length, buffer);

    assertThat(deserialized).isEqualTo(value);
  }

  @Test
  void bigDecimalSerialization () {
    val value = new BigDecimal("-48938742.15034");
    val buffer = Bytes.resizableArray();

    Serializer.BIG_DECIMAL.serialize(value, buffer);
    val length = buffer.writerIndex();
    val deserialized = Deserializer.BIG_DECIMAL.deserialize(length, buffer);

    assertThat(deserialized).isEqualTo(value);
  }

  @Test
  void byteArraySerialization () {
    val value = new byte[1024];
    ThreadLocalRandom.current().nextBytes(value);
    val buffer = Bytes.resizableArray();

    Serializer.BYTE_ARRAY.serialize(value, buffer);
    val length = buffer.writerIndex();
    val deserialized = Deserializer.BYTE_ARRAY.deserialize(length, buffer);

    assertThat(deserialized).isEqualTo(value);
  }

  @Test
  void stringSerialization () {
    val value = "Hello world! How are you? I am fine, thanks.";
    val buffer = Bytes.resizableArray();

    Serializer.STRING.serialize(value, buffer);
    val length = buffer.writerIndex();
    val deserialized = Deserializer.STRING.deserialize(length, buffer);

    assertThat(deserialized).isEqualTo(value);
  }

  @Test
  void complexSerialization () {
    val buffer = Bytes.resizableArray();

    val value1 = "Artem";
    val value2 = new BigDecimal("29.004");
    val value3 = new byte[] { 3, 8, 10 };

    Serializer.STRING.serialize(value1, buffer);
    Serializer.BIG_DECIMAL.serialize(value2, buffer);
    Serializer.BYTE_ARRAY.serialize(value3, buffer);

    val result1 = Deserializer.STRING.deserialize(0, buffer);
    assertThat(result1).isEqualTo(value1);

    val result2 = Deserializer.BIG_DECIMAL.deserialize(0, buffer);
    assertThat(result2).isEqualTo(value2);

    val result3 = Deserializer.BYTE_ARRAY.deserialize(0, buffer);
    assertThat(result3).isEqualTo(value3);
  }
}

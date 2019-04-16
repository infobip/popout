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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;

import io.appulse.utils.Bytes;
import io.appulse.utils.SerializationUtils;
import lombok.val;

public interface Serializer<T> {

  Serializer<Byte> BYTE = new ByteSerializer();

  Serializer<Short> SHORT = new ShortSerializer();

  Serializer<Character> CHARACTER = new CharacterSerializer();

  Serializer<Integer> INTEGER = new IntegerSerializer();

  Serializer<Long> LONG = new LongSerializer();

  Serializer<Float> FLOAT = new FloatSerializer();

  Serializer<Double> DOUBLE = new DoubleSerializer();

  Serializer<BigInteger> BIG_INTEGER = new BigIntegerSerializer();

  Serializer<BigDecimal> BIG_DECIMAL = new BigDecimalSerializer();

  Serializer<byte[]> BYTE_ARRAY = new ByteArraySerializer();

  Serializer<String> STRING = new StringSerializer();

  void serialize (T object, Bytes buffer);

  class DefaultSerializer<T> implements Serializer<T> {

    @Override
    public void serialize (T object, Bytes buffer) {
      if (!(object instanceof Serializable)) {
        val msg = "Default serializer allows only objects which implement java.io.Serializable interface";
        throw new IllegalArgumentException(msg);
      }
      val serializable = (Serializable) object;
      val bytes = SerializationUtils.serialize(serializable);
      BYTE_ARRAY.serialize(bytes, buffer);
    }
  }

  class ByteSerializer implements Serializer<Byte> {

    @Override
    public void serialize (Byte object, Bytes buffer) {
      buffer.write1B(object);
    }
  }

  class ShortSerializer implements Serializer<Short> {

    @Override
    public void serialize (Short object, Bytes buffer) {
      buffer.write2B(object);
    }
  }

  class CharacterSerializer implements Serializer<Character> {

    @Override
    public void serialize (Character object, Bytes buffer) {
      buffer.write2B(object);
    }
  }

  class IntegerSerializer implements Serializer<Integer> {

    @Override
    public void serialize (Integer object, Bytes buffer) {
      buffer.write4B(object);
    }
  }

  class LongSerializer implements Serializer<Long> {

    @Override
    public void serialize (Long object, Bytes buffer) {
      buffer.write8B(object);
    }
  }

  class FloatSerializer implements Serializer<Float> {

    @Override
    public void serialize (Float object, Bytes buffer) {
      buffer.write4B(object);
    }
  }

  class DoubleSerializer implements Serializer<Double> {

    @Override
    public void serialize (Double object, Bytes buffer) {
      buffer.write8B(object);
    }
  }

  class BigIntegerSerializer implements Serializer<BigInteger> {

    @Override
    public void serialize (BigInteger object, Bytes buffer) {
      val bytes = object.toByteArray();
      buffer.write4B(bytes.length);
      buffer.writeNB(bytes);
    }
  }

  class BigDecimalSerializer implements Serializer<BigDecimal> {

    @Override
    public void serialize (BigDecimal object, Bytes buffer) {
      val unscaledValue = object.unscaledValue();
      val bytes = unscaledValue.toByteArray();
      buffer.write4B(bytes.length);
      buffer.writeNB(bytes);
      buffer.write4B(object.scale());
    }
  }

  class ByteArraySerializer implements Serializer<byte[]> {

    @Override
    public void serialize (byte[] object, Bytes buffer) {
      buffer.write4B(object.length);
      buffer.writeNB(object);
    }
  }

  class StringSerializer implements Serializer<String> {

    @Override
    public void serialize (String object, Bytes buffer) {
      val bytes = object.getBytes(UTF_8);
      buffer.write4B(bytes.length);
      buffer.writeNB(bytes);
    }
  }
}

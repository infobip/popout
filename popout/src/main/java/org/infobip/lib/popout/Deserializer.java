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

import java.math.BigDecimal;
import java.math.BigInteger;

import io.appulse.utils.Bytes;
import io.appulse.utils.SerializationUtils;
import lombok.val;

public interface Deserializer<T> {

  Deserializer<Byte> BYTE = new ByteDeserializer();

  Deserializer<Short> SHORT = new ShortDeserializer();

  Deserializer<Character> CHARACTER = new CharacterDeserializer();

  Deserializer<Integer> INTEGER = new IntegerDeserializer();

  Deserializer<Long> LONG = new LongDeserializer();

  Deserializer<Float> FLOAT = new FloatDeserializer();

  Deserializer<Double> DOUBLE = new DoubleDeserializer();

  Deserializer<BigInteger> BIG_INTEGER = new BigIntegerDeserializer();

  Deserializer<BigDecimal> BIG_DECIMAL = new BigDecimalDeserializer();

  Deserializer<byte[]> BYTE_ARRAY = new ByteArrayDeserializer();

  Deserializer<String> STRING = new StringDeserializer();

  T deserialize (int length, Bytes buffer);

  class DefaultDeserializer<T> implements Deserializer<T> {

    @Override
    public T deserialize (int length, Bytes buffer) {
      val bytes = BYTE_ARRAY.deserialize(length, buffer);
      return SerializationUtils.deserialize(bytes);
    }
  }

  class ByteDeserializer implements Deserializer<Byte> {

    @Override
    public Byte deserialize (int length, Bytes buffer) {
      return buffer.readByte();
    }
  }

  class ShortDeserializer implements Deserializer<Short> {

    @Override
    public Short deserialize (int length, Bytes buffer) {
      return buffer.readShort();
    }
  }

  class CharacterDeserializer implements Deserializer<Character> {

    @Override
    public Character deserialize (int length, Bytes buffer) {
      return buffer.readChar();
    }
  }

  class IntegerDeserializer implements Deserializer<Integer> {

    @Override
    public Integer deserialize (int length, Bytes buffer) {
      return buffer.readInt();
    }
  }

  class LongDeserializer implements Deserializer<Long> {

    @Override
    public Long deserialize (int length, Bytes buffer) {
      return buffer.readLong();
    }
  }

  class FloatDeserializer implements Deserializer<Float> {

    @Override
    public Float deserialize (int length, Bytes buffer) {
      return buffer.readFloat();
    }
  }

  class DoubleDeserializer implements Deserializer<Double> {

    @Override
    public Double deserialize (int length, Bytes buffer) {
      return buffer.readDouble();
    }
  }

  class BigIntegerDeserializer implements Deserializer<BigInteger> {

    @Override
    public BigInteger deserialize (int length, Bytes buffer) {
      val arrayLength = buffer.readInt();
      val bytes = buffer.readBytes(arrayLength);
      return new BigInteger(bytes);
    }
  }

  class BigDecimalDeserializer implements Deserializer<BigDecimal> {

    @Override
    public BigDecimal deserialize (int length, Bytes buffer) {
      val arrayLength = buffer.readInt();
      val bytes = buffer.readBytes(arrayLength);
      val scale = buffer.readInt();

      val unscaledValue = new BigInteger(bytes);
      return new BigDecimal(unscaledValue, scale);
    }
  }

  class ByteArrayDeserializer implements Deserializer<byte[]> {

    @Override
    public byte[] deserialize (int length, Bytes buffer) {
      val arrayLength = buffer.readInt();
      return buffer.readBytes(arrayLength);
    }
  }

  class StringDeserializer implements Deserializer<String> {

    @Override
    public String deserialize (int length, Bytes buffer) {
      val arrayLength = buffer.readInt();
      val bytes = buffer.readBytes(arrayLength);
      return new String(bytes, UTF_8);
    }
  }
}

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

package org.infobip.lib.popout.backend;

import static lombok.AccessLevel.PRIVATE;
import static org.infobip.lib.popout.backend.RecordHeader.Marker.END;
import static org.infobip.lib.popout.backend.RecordHeader.Marker.JUMP;
import static org.infobip.lib.popout.backend.RecordHeader.Marker.RECORD;

import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.stream.Stream;

import io.appulse.utils.ReadBytesUtils;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.val;

@FieldDefaults(level = PRIVATE, makeFinal = true)
class RecordHeader {

  static final int BYTES = Byte.BYTES + Long.BYTES; // marker+length

  ByteBuffer buffer = ByteBuffer.allocate(BYTES);

  RecordHeader readFrom (FileChannel channel) {
    buffer.clear();
    ReadBytesUtils.read(channel, buffer);
    return this;
  }

  @SneakyThrows
  RecordHeader skipJumps (FileChannel channel) {
    do {
      readFrom(channel);
      if (!isJump()) {
        return this;
      }
      val newPosition = getValue();
      channel.position(newPosition);
    } while (true);
  }

  @SneakyThrows
  void writeJump (FileChannel channel, long from, long to) {
    buffer.clear();
    buffer.put(JUMP.getValue());
    buffer.putLong(to);
    buffer.rewind();
    channel.write(buffer, from);
  }

  @SneakyThrows
  void writeRecord (FileChannel channel, long length) {
    buffer.clear();
    buffer.put(RECORD.getValue());
    buffer.putLong(length);
    buffer.rewind();
    channel.write(buffer);
  }

  @SneakyThrows
  void writeEnd (FileChannel channel) {
    buffer.clear();
    buffer.put(END.getValue());
    buffer.putLong(0);
    buffer.rewind();
    channel.write(buffer);
  }

  Marker getMarker () {
    val value = buffer.get(0);
    return Marker.of(value);
  }

  long getValue () {
    return buffer.getLong(1);
  }

  int getLength () {
    return (int) getValue();
  }

  boolean isRecord () {
    if (getMarker() != RECORD) {
      return false;
    }
    if (getValue() == 0) {
      throw new IllegalStateException();
    }
    return true;
  }

  boolean isJump () {
    if (getMarker() != JUMP) {
      return false;
    }
    if (getValue() == 0) {
      throw new IllegalStateException();
    }
    return true;
  }

  boolean isEnd () {
    if (getMarker() != END) {
      return false;
    }
    if (getValue() != 0) {
      throw new IllegalStateException();
    }
    return true;
  }

  enum Marker {

    RECORD(1),
    JUMP(2),
    END(4),
    UNDEFINED(0xFF);

    byte value;

    Marker (int value) {
      this.value = (byte) value;
    }

    byte getValue () {
      return value;
    }

    static Marker of (byte value) {
      return Stream.of(values())
          .filter(it -> it.getValue() == value)
          .findAny()
          .orElse(UNDEFINED);
    }
  }
}

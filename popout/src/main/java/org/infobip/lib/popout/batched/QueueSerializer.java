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

package org.infobip.lib.popout.batched;

import static java.util.stream.Collectors.toCollection;
import static lombok.AccessLevel.PRIVATE;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import java.util.stream.IntStream;

import org.infobip.lib.popout.Deserializer;
import org.infobip.lib.popout.Serializer;
import org.infobip.lib.popout.backend.WalContent;

import io.appulse.utils.Bytes;
import io.appulse.utils.ReadBytesUtils;
import lombok.Builder;
import lombok.experimental.FieldDefaults;
import lombok.val;

@FieldDefaults(level = PRIVATE, makeFinal = true)
class QueueSerializer<T> {

  Serializer<T> serializer;

  Deserializer<T> deserializer;

  @Builder
  QueueSerializer (Serializer<T> serializer, Deserializer<T> deserializer) {
    this.serializer = serializer;
    this.deserializer = deserializer;
  }

  void serialize (Queue<T> collection, Bytes buffer) {
    buffer.reset()
        .write4B(collection.size());

    collection.forEach(item -> {
      val writerIndex = buffer.writerIndex();
      buffer.write4B(0); // write fake length
      serializer.serialize(item, buffer);
      buffer.set4B(writerIndex, buffer.writerIndex() - writerIndex - Integer.BYTES); // write real length
    });
  }

  Queue<T> deserialize (Bytes buffer) {
    return IntStream.range(0, buffer.readInt())
        .mapToObj(it -> deserializeItem(buffer))
        .filter(Objects::nonNull)
        .collect(toCollection(LinkedList::new));
  }

  int getQueueLength (WalContent walContent) {
    val lengthBuffer = ByteBuffer.allocate(Integer.BYTES);
    walContent.open((length, channel) -> {
      ReadBytesUtils.read(channel, lengthBuffer);
    });
    lengthBuffer.flip();
    return lengthBuffer.getInt();
  }

  Iterator<T> toIterator (WalContent walContent) {
    return new WalContentIterator(walContent);
  }

  private T deserializeItem (Bytes buffer) {
    int length = 0;
    while (buffer.isReadable(Integer.BYTES)) {
      // we are skipping removed items.
      // item is removed if its length is negative.
      // Math.abs(-length) - bytes to skip.
      length = buffer.readInt();
      if (length >= 0) {
        break;
      }
      val jump = Math.abs(length);
      if (buffer.isReadable(jump)) {
        return null;
      }
      val newReaderPosition = buffer.readerIndex() + jump;
      buffer.readerIndex(newReaderPosition);
    }
    return deserializer.deserialize(length, buffer);
  }

  private class WalContentIterator implements Iterator<T> {

    WalContent walContent;

    int index;

    int elements;

    long currentPosition;

    long nextPosition;

    Bytes buffer;

    T nextItem;

    WalContentIterator (WalContent walContent) {
      this.walContent = walContent;
      elements = getQueueLength(walContent);
      nextPosition = walContent.getOffset() + Integer.BYTES;
      buffer = Bytes.resizableArray(32);
    }

    @Override
    @SuppressWarnings({
        "PMD.AccessorMethodGeneration",
        "PMD.NPathComplexity"
    })
    public boolean hasNext () {
      if (nextItem != null) {
        return true;
      }
      if (index >= elements) {
        return false;
      }

      walContent.open((walContentLength, channel) -> {
        val limit = channel.position() + walContentLength;
        val minRecordLength = Integer.BYTES;

        if (limit <= nextPosition + minRecordLength) {
          return;
        }

        int length;
        do {
          channel.position(nextPosition);
          buffer.reset();
          val readed = ReadBytesUtils.read(channel, buffer, Integer.BYTES);
          if (readed < Integer.BYTES) {
            throw new IllegalStateException();
          }

          length = buffer.readInt();
          if (length >= 0) {
            break;
          }
          nextPosition += Integer.BYTES + Math.abs(length);
          if (limit <= nextPosition + minRecordLength) {
            return;
          }
        } while (true);
        if (!buffer.isWritable(length)) {
          val newCapacity = buffer.writerIndex() + length;
          buffer.capacity(newCapacity);
        }
        val readed = ReadBytesUtils.read(channel, buffer, length);
        if (readed < length) {
          throw new IllegalStateException();
        }

        buffer.readerIndex(0);
        nextItem = deserializeItem(buffer);

        currentPosition = nextPosition;
        nextPosition = channel.position();
      });
      return nextItem != null;
    }

    @Override
    public T next () {
      if (nextItem != null || hasNext()) {
        val result = nextItem;
        nextItem = null;
        index++;
        return result;
      }
      throw new NoSuchElementException();
    }

    @Override
    public void remove () {
      walContent.open((walContentLength, channel) -> {
        channel.position(currentPosition);

        val buf = ByteBuffer.allocate(Integer.BYTES);
        val readed = channel.read(buf);
        if (readed < Integer.BYTES) {
          throw new IllegalStateException();
        }
        buf.flip();

        val length = buf.getInt();
        buf.putInt(0, -length);
        buf.flip();

        channel.position(currentPosition);
        val written = channel.write(buf);
        if (written < Integer.BYTES) {
          throw new IllegalStateException();
        }

        nextPosition = channel.position() + length;
      });
    }
  }
}

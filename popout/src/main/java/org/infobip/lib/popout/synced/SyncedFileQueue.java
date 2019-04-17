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

package org.infobip.lib.popout.synced;

import static lombok.AccessLevel.PRIVATE;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.LongAdder;

import org.infobip.lib.popout.FileQueue;
import org.infobip.lib.popout.QueueLimit;
import org.infobip.lib.popout.ReadWriteBytesPool;
import org.infobip.lib.popout.backend.FileSystemBackend;
import org.infobip.lib.popout.backend.WalContent;

import io.appulse.utils.Bytes;
import io.appulse.utils.ReadBytesUtils;
import lombok.NonNull;
import lombok.Synchronized;
import lombok.experimental.FieldDefaults;
import lombok.val;

@FieldDefaults(level = PRIVATE, makeFinal = true)
class SyncedFileQueue<T> extends FileQueue<T> {

  LongAdder size;

  FileSystemBackend backend;

  ItemSerialization<T> serialization;

  QueueLimit<T> limit;

  SyncedFileQueue (@NonNull SyncedFileQueueBuilder<T> builder) {
    super();

    limit = builder.getLimit();

    serialization = ItemSerialization.<T>builder()
        .serializer(builder.getSerializer())
        .deserializer(builder.getDeserializer())
        .build();

    backend = FileSystemBackend.builder()
        .queueName(builder.getName())
        .restoreFromDisk(builder.isRestoreFromDisk())
        .walConfig(builder.getWalFilesConfig())
        .compressedConfig(builder.getCompressedFilesConfig())
        .build();

    size = new LongAdder();
    val iterator = backend.iterator();
    while (iterator.hasNext()) {
      size.increment();
    }
  }

  @Override
  @Synchronized
  public boolean offer (@NonNull T value) {
    if (limit.isExceeded(this)) {
      limit.handle(value, this);
      return false;
    }

    ReadWriteBytesPool.getInstance().borrow(buffer -> {
      serialization.serialize(value, buffer);
      backend.write(buffer);
      return null;
    });
    size.increment();
    return true;
  }

  @Override
  @Synchronized
  public T poll () {
    return ReadWriteBytesPool.getInstance().borrow(buffer -> {
      val readed = backend.pollTo(buffer);
      if (readed <= 0) {
        return null;
      }
      size.decrement();
      return serialization.deserialize(buffer);
    });
  }

  @Override
  @Synchronized
  public T peek () {
    return ReadWriteBytesPool.getInstance().borrow(buffer -> {
      val readed = backend.peakTo(buffer);
      return readed > 0
            ? serialization.deserialize(buffer)
            : null;
    });
  }

  @Override
  public int size () {
    return size.intValue();
  }

  @Override
  public long longSize () {
    return size.longValue();
  }

  @Override
  public long diskSize () {
    return backend.diskSize();
  }

  @Override
  public void flush () {
    // no op, we always in a synced state with a disk
  }

  @Override
  @Synchronized
  public void compress () {
    backend.compress();
  }

  @Override
  public Iterator<T> iterator () {
    return new SyncedFileQueueIterator();
  }

  @Override
  public void close () {
    // nothing to close
  }

  private class SyncedFileQueueIterator implements Iterator<T> {

    Iterator<WalContent> walContentsIterator = backend.iterator();

    Bytes buffer = Bytes.resizableArray(32);

    WalContent nextWalContent;

    @Override
    public boolean hasNext () {
      if (nextWalContent != null) {
        return true;
      }
      if (!walContentsIterator.hasNext()) {
        return false;
      }
      nextWalContent = walContentsIterator.next();
      return true;
    }

    @Override
    public T next () {
      if (nextWalContent != null || hasNext()) {
        val walContent = nextWalContent;
        nextWalContent = null;
        walContent.open((length, channel) -> {
          buffer.reset();
          if (!buffer.isWritable(length)) {
            val newCapacity = buffer.writerIndex() + length;
            buffer.capacity(newCapacity);
          }
          ReadBytesUtils.read(channel, buffer);
        });
        return serialization.deserialize(buffer);
      }
      throw new NoSuchElementException();
    }

    @Override
    public void remove () {
      walContentsIterator.remove();
      size.decrement();
    }
  }
}

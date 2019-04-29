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

import static lombok.AccessLevel.PRIVATE;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import org.infobip.lib.popout.FileQueue;
import org.infobip.lib.popout.QueueLimit;
import org.infobip.lib.popout.ReadWriteBytesPool;
import org.infobip.lib.popout.backend.FileSystemBackend;
import org.infobip.lib.popout.backend.WalContent;

import io.appulse.utils.LimitedQueue;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.val;

@FieldDefaults(level = PRIVATE, makeFinal = true)
class BatchedFileQueue<T> extends FileQueue<T> {

  LongAdder size;

  Queue<T> tail;

  FileSystemBackend backend;

  @NonFinal
  Queue<T> head;

  QueueSerializer<T> queueSerializer;

  QueueLimit<T> limit;

  Lock writeLock;

  Lock readLock;

  BatchedFileQueue (@NonNull BatchedFileQueueBuilder<T> builder) {
    super();

    backend = FileSystemBackend.builder()
        .queueName(builder.getName())
        .restoreFromDisk(builder.isRestoreFromDisk())
        .walConfig(builder.getWalFilesConfig())
        .compressedConfig(builder.getCompressedFilesConfig())
        .corruptionHandler(builder.getCorruptionHandler())
        .build();

    queueSerializer = QueueSerializer.<T>builder()
        .serializer(builder.getSerializer())
        .deserializer(builder.getDeserializer())
        .build();

    size = new LongAdder();

    head = new LinkedList<>();
    tail = new LimitedQueue<>(builder.getBatchSize());

    val iterator = backend.iterator();
    while (iterator.hasNext()) {
      val walContent = iterator.next();
      val length = queueSerializer.getQueueLength(walContent);
      size.add(length);
    }

    limit = builder.getLimit();
    writeLock = new ReentrantLock(true);
    readLock = new ReentrantLock(true);
  }

  @Override
  public boolean offer (@NonNull T value) {
    if (limit.isExceeded(this)) {
      limit.handle(value, this);
      return false;
    }

    writeLock.lock();
    try {
      if (tail.offer(value)) {
        size.increment();
        return true;
      }
      flush();
      tail.add(value);
      size.increment();
    } finally {
      writeLock.unlock();
    }
    return true;
  }

  @Override
  public T poll () {
    val result = doOn(Queue::poll);
    if (result != null) {
      size.decrement();
    }
    return result;
  }

  @Override
  public T peek () {
    return doOn(Queue::peek);
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
    writeLock.lock();
    try {
      if (tail.isEmpty()) {
        return;
      }
      ReadWriteBytesPool.getInstance().borrow(buffer -> {
        queueSerializer.serialize(tail, buffer);
        backend.write(buffer);
        return null;
      });
      tail.clear();
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public void compress () {
    writeLock.lock();
    try {
      backend.compress();
    } finally {
      writeLock.unlock();
    }
  }

  @Override
  public Iterator<T> iterator () {
    return new BatchedFileQueueIterator();
  }

  @Override
  public void close () {
    flush();
    backend.close();
  }

  private T doOn (Function<Queue<T>, T> extractor) {
    readLock.lock();
    try {
      T result = extractor.apply(head);
      if (result != null) {
        return result;
      }

      writeLock.lock();
      try {
        return ReadWriteBytesPool.getInstance().borrow(buffer -> {
          if (backend.pollTo(buffer) <= 0) {
            return extractor.apply(tail);
          }
          head = queueSerializer.deserialize(buffer);
          return extractor.apply(head);
        });
      } finally {
        writeLock.unlock();
      }
    } finally {
      readLock.unlock();
    }
  }

  private class BatchedFileQueueIterator implements Iterator<T> {

    Iterator<T> current = head.iterator();

    Iterator<T> backendIterator = new BackendIterator();

    Iterator<T> tailIterator = tail.iterator();

    @Override
    public boolean hasNext () {
      if (current.hasNext()) {
        return true;
      } else if (backendIterator.hasNext()) {
        current = backendIterator;
        return true;
      } else if (tailIterator.hasNext()) {
        current = tailIterator;
        return true;
      }
      return false;
    }

    @Override
    public T next () {
      return current.next();
    }

    @Override
    public void remove () {
      current.remove();
      size.decrement();
    }
  }

  private class BackendIterator implements Iterator<T> {

    Iterator<WalContent> walContentsIterator = backend.iterator();

    Iterator<T> elements;

    @Override
    public boolean hasNext () {
      if (elements == null || !elements.hasNext()) {
        if (!walContentsIterator.hasNext()) {
          return false;
        }
        val walContent = walContentsIterator.next();
        elements = queueSerializer.toIterator(walContent);
      }
      return elements.hasNext();
    }

    @Override
    public T next () {
      return elements.next();
    }

    @Override
    public void remove () {
      elements.remove();
    }
  }
}

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

import static java.util.Optional.ofNullable;
import static lombok.AccessLevel.PRIVATE;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.infobip.lib.popout.FileQueue;
import org.infobip.lib.popout.WalFilesConfig;
import org.infobip.lib.popout.exception.CorruptedDataException;

import io.appulse.utils.Bytes;
import io.appulse.utils.ReadBytesUtils;
import io.appulse.utils.WriteBytesUtils;
import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.val;

@FieldDefaults(level = PRIVATE, makeFinal = true)
class WalFiles implements Iterable<WalContent>, AutoCloseable {

  FilesManager files;

  int maxCount;

  Function<CorruptedDataException, Boolean> corruptionHandler;

  @Builder
  WalFiles (@NonNull String queueName,
            @NonNull WalFilesConfig config,
            Boolean restoreFromDisk,
            Function<CorruptedDataException, Boolean> corruptionHandler
  ) {
    val restoreFromDiskValue = ofNullable(restoreFromDisk)
        .orElse(Boolean.TRUE);

    val corruptionHandlerValue = ofNullable(corruptionHandler)
        .orElseGet(() -> new FileQueue.DefaultCorruptionHandler());

    files = FilesManager.builder()
        .folder(config.getFolder())
        .prefix(queueName + '-')
        .suffix(".wal")
        .build();

    if (!restoreFromDiskValue) {
      files.clear();
    }

    maxCount = config.getMaxCount();
    this.corruptionHandler = corruptionHandlerValue;
  }

  @Override
  public void close () {
    files.close();
  }

  void write (@NonNull Bytes buffer) {
    val file = files.createNextFile();
    WriteBytesUtils.write(file, buffer);
  }

  @SneakyThrows
  int pollTo (@NonNull Bytes buffer) {
    return readTo(buffer, files::poll, files::remove);
  }

  @SneakyThrows
  int peakTo (@NonNull Bytes buffer) {
    return readTo(buffer, files::peek, null);
  }

  boolean isLimitExceeded () {
    return files.getFilesFromQueue().size() > maxCount;
  }

  Collection<Path> getFiles () {
    return files.getFilesFromQueue();
  }

  @SneakyThrows
  long diskSize () {
    long result = 0;
    for (val file : getFiles()) {
      result += Files.size(file);
    }
    return result;
  }

  void remove (Collection<Path> paths) {
    files.remove(paths);
  }

  @Override
  public Iterator<WalContent> iterator () {
    return new WalFilesIterator();
  }

  @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
  private int readTo (Bytes buffer, Supplier<Path> supplier, Consumer<Path> actionAfter) {
    val writerIndex = buffer.writerIndex();
    val readerIndex = buffer.readerIndex();
    do {
      val file = supplier.get();
      try {
        if (file == null) {
          return 0;
        }

        val size = (int) Files.size(file);
        if (!buffer.isWritable(size)) {
          val newCapacity = buffer.writerIndex() + size;
          buffer.capacity(newCapacity);
        }

        val readed = ReadBytesUtils.read(file, buffer);
        if (readed > 0 && actionAfter != null) {
          actionAfter.accept(file);
        }
        return readed;
      } catch (Exception ex) {
        val exception = new CorruptedDataException(file, 0, ex);
        corruptionHandler.apply(exception);
        files.remove(file);
      }
      buffer.writerIndex(writerIndex);
      buffer.readerIndex(readerIndex);
    } while (true);
  }

  @FieldDefaults(level = PRIVATE)
  private class WalFilesIterator implements Iterator<WalContent> {

    final Iterator<Path> paths = files.getFilesFromQueue().iterator();

    WalContent lastReturned;

    WalContent next;

    @Override
    @SneakyThrows
    public boolean hasNext () {
      if (next != null) {
        return true;
      }
      if (!paths.hasNext()) {
        return false;
      }
      val path = paths.next();
      next = WalContent.builder()
          .file(path)
          .offset(0)
          .length((int) Files.size(path))
          .build();

      return true;
    }

    @Override
    public WalContent next () {
      if (next != null || hasNext()) {
        lastReturned = next;
        next = null;
        return lastReturned;
      }
      throw new NoSuchElementException();
    }

    @Override
    @SneakyThrows
    public void remove () {
      if (lastReturned == null) {
        throw new IllegalStateException();
      }
      paths.remove();
      Files.deleteIfExists(lastReturned.getFile());
      lastReturned = null;
    }
  }
}

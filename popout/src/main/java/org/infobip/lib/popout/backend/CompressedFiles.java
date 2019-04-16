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

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;
import static lombok.AccessLevel.PRIVATE;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.infobip.lib.popout.CompressedFilesConfig;

import io.appulse.utils.Bytes;
import io.appulse.utils.ReadBytesUtils;
import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.val;

@FieldDefaults(level = PRIVATE, makeFinal = true)
class CompressedFiles implements Iterable<WalContent> {

  private static final byte[] CLEAR = new byte[8192];

  FilesManager files;

  long maxFileSizeBytes;

  @Builder
  CompressedFiles (@NonNull String queueName,
                   @NonNull Boolean restoreFromDisk,
                   @NonNull CompressedFilesConfig config
  ) {
    files = FilesManager.builder()
        .folder(config.getFolder())
        .prefix(queueName + '-')
        .suffix(".compressed")
        .build();

    if (!restoreFromDisk) {
      files.clear();
    }

    maxFileSizeBytes = config.getMaxSizeBytes();
  }

  @SneakyThrows
  int peekContentPart (@NonNull Bytes buffer) {
    return readContent((channel, header) -> {
      val length = header.getLength();
      if (buffer.isWritable(length)) {
        val newCapacity = buffer.writerIndex() + length;
        buffer.capacity(newCapacity);
      }
      return ReadBytesUtils.read(channel, buffer, length);
    });
  }

  @SneakyThrows
  int pollContentPart (@NonNull Bytes buffer) {
    return readContent((channel, header) -> {
      val length = header.getLength();
      if (!buffer.isWritable(length)) {
        val newCapacity = buffer.writerIndex() + length;
        buffer.capacity(newCapacity);
      }

      val result = ReadBytesUtils.read(channel, buffer, length);

      val position = channel.position();
      if (!header.skipJumps(channel).isEnd()) {
        // jump from the begining of file to the current position:
        header.writeJump(channel, 0, position);
      }
      return result;
    });
  }

  @SneakyThrows
  private int readContent (RecordReader reader) {
    val header = new RecordHeader();
    do {
      val file = files.peek();
      if (file == null) {
        return 0;
      }

      boolean shouldRemoveFile = false;
      try (val channel = FileChannel.open(file, READ, WRITE)) {
        header.skipJumps(channel);
        if (header.isEnd()) {
          shouldRemoveFile = true;
        } else {
          val readed = reader.read(channel, header);
          if (readed == 0 || header.isEnd()) {
            shouldRemoveFile = true;
          }
          if (readed > 0) {
            return readed;
          }
        }
      } finally {
        if (shouldRemoveFile) {
          files.remove(file);
        }
      }
    } while (true);
  }

  @SneakyThrows
  CompressionResult compress (@NonNull Collection<Path> walFiles) {
    val result = new CompressionResult(new ArrayList<>(), new ArrayList<>(walFiles));
    if (walFiles.isEmpty()) {
      return result;
    }

    val file = files.createNextFile();
    val walFilesSumSize = walFiles.stream()
        .mapToLong(this::getSizeWithHeader)
        .sum() + RecordHeader.BYTES;

    val needToAllocate = Math.min(walFilesSumSize, maxFileSizeBytes);
    val allocated = allocate(file, needToAllocate) - RecordHeader.BYTES;

    val header = new RecordHeader();
    try (val channel = FileChannel.open(file, WRITE)) {
      for (val walFile : walFiles) {
        val size = Files.size(walFile);
        if (channel.position() + RecordHeader.BYTES + size > allocated) {
          break;
        }
        header.writeRecord(channel, size);

        try (val walFileChannel = FileChannel.open(walFile)) {
          walFileChannel.transferTo(0, walFileChannel.size(), channel);
        }

        result.getCompressed().add(walFile);
        result.getRemaining().remove(walFile);
      }
      header.writeEnd(channel);
    }
    return result;
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

  @Override
  public Iterator<WalContent> iterator () {
    return new CompressedFileIteratorManyFiles(files.getFilesFromQueue());
  }

  @SneakyThrows
  private long getSizeWithHeader (Path path) {
    return RecordHeader.BYTES + Files.size(path);
  }

  @SneakyThrows
  @SuppressWarnings("PMD.UnusedPrivateMethod")
  private long allocate (Path path, long size) {
    int allocated = 0;
    try (val out = Files.newOutputStream(path, CREATE, WRITE)) {
      while (allocated < size) {
        val bufferSize = (int) Math.min(CLEAR.length, size - allocated);
        out.write(CLEAR, 0, bufferSize);
        allocated += bufferSize;
      }
    }
    return allocated;
  }

  @FunctionalInterface
  interface RecordReader {

    int read (FileChannel channel, RecordHeader header) throws IOException;
  }
}

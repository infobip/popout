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
import static java.util.Optional.ofNullable;
import static lombok.AccessLevel.PRIVATE;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Function;

import org.infobip.lib.popout.CompressedFilesConfig;
import org.infobip.lib.popout.FileQueue;
import org.infobip.lib.popout.exception.CorruptedDataException;

import io.appulse.utils.Bytes;
import io.appulse.utils.ReadBytesUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.val;

@FieldDefaults(level = PRIVATE, makeFinal = true)
class CompressedFiles implements Iterable<WalContent>, AutoCloseable {

  private static final byte[] CLEAR = new byte[8192];

  FilesManager files;

  long maxFileSizeBytes;

  Function<CorruptedDataException, Boolean> corruptionHandler;

  @Builder
  CompressedFiles (@NonNull String queueName,
                   @NonNull CompressedFilesConfig config,
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
        .suffix(".compressed")
        .build();

    if (!restoreFromDiskValue) {
      files.clear();
    }

    maxFileSizeBytes = config.getMaxSizeBytes();
    this.corruptionHandler = corruptionHandlerValue;
  }

  @Override
  public Iterator<WalContent> iterator () {
    return new CompressedFileIteratorManyFiles(files.getFilesFromQueue());
  }

  @Override
  public void close () {
    files.close();
  }

  @SneakyThrows
  int peekContentPart (@NonNull Bytes bytes) {
    return readTo(bytes, (channel, header, buffer) -> {
      val length = header.getLength();
      if (buffer.isWritable(length)) {
        val newCapacity = buffer.writerIndex() + length;
        buffer.capacity(newCapacity);
      }
      return ReadBytesUtils.read(channel, buffer, length);
    });
  }

  @SneakyThrows
  int pollContentPart (@NonNull Bytes bytes) {
    return readTo(bytes, (channel, header, buffer) -> {
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

  private int readTo (Bytes buffer, RecordReader reader) {
    val writerIndex = buffer.writerIndex();
    val readerIndex = buffer.readerIndex();

    RecordHeader header = new RecordHeader();
    do {
      Path file = files.peek();
      if (file == null) {
        return 0;
      }

      val result = readTo(file, header, buffer, reader);
      if (result.isRemoveFile()) {
        files.remove(file);
      }
      if (result.hesReaded()) {
        return (int) result.getReaded();
      }

      buffer.writerIndex(writerIndex);
      buffer.readerIndex(readerIndex);
    } while (true);
  }

  @SneakyThrows
  private ReadResult readTo (Path file, RecordHeader header, Bytes buffer, RecordReader reader) {
    FileChannel channel = null;
    try {
      channel = FileChannel.open(file, READ, WRITE);

      header.skipJumps(channel);
      if (header.isEnd()) {
        return ReadResult.endOfFile();
      }

      val readed = reader.read(channel, header, buffer);
      boolean shouldRemoveFile = readed == 0 || header.isEnd();
      return new ReadResult(readed, shouldRemoveFile);
    } catch (Exception ex) {
      val offset = channel == null
                   ? 0
                   : channel.position();

      val exception = new CorruptedDataException(file, offset, ex);
      return corruptionHandler.apply(exception)
             ? ReadResult.endOfFile()
             : ReadResult.continueReading(offset);
    } finally {
      if (channel != null) {
        channel.close();
      }
    }
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

    int read (FileChannel channel, RecordHeader header, Bytes buffer) throws IOException;
  }

  @Value
  @AllArgsConstructor
  static class ReadResult {

    static ReadResult endOfFile () {
      return new ReadResult(0, true);
    }

    static ReadResult continueReading (long readed) {
      return new ReadResult(readed, false);
    }

    long readed;

    boolean removeFile;

    boolean hesReaded () {
      return readed > 0;
    }
  }
}

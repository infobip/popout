/*
 * Copyright 2018 Infobip Ltd.
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

package org.infobip.lib.popout.reader;

import static java.nio.channels.FileChannel.MapMode.READ_ONLY;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static lombok.AccessLevel.PRIVATE;
import static org.infobip.lib.popout.util.ReflectionUtils.getFieldValueFrom;
import static org.infobip.lib.popout.util.ReflectionUtils.invokeMethodOf;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.Optional;

import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.val;

/**
 * Mmap based implementation of {@link FileReader} based on {@link MappedByteBuffer}.
 *
 * @since 1.0.0
 * @author Artem Labazin
 */
@FieldDefaults(level = PRIVATE, makeFinal = true)
public final class MmapFileReader implements FileReader {

    Path path;

    FileChannel channel;

    int bufferSize;

    @NonFinal
    MappedByteBuffer buffer;

    @NonFinal
    long offset;

    /**
     * Mmap reader constructor.
     *
     * @param path       a file's path to read
     * @param bufferSize buffer's size in bytes. Default is 8192 bytes (8Kb)
     */
    @Builder
    @SneakyThrows
    public MmapFileReader (@NonNull Path path, Integer bufferSize) {
        this.path = path;
        this.bufferSize = ofNullable(bufferSize).orElse(8192);

        channel = FileChannel.open(path, CREATE, READ);
    }

    @Override
    @SneakyThrows
    public long currentFileSize () {
        return channel.size();
    }

    @Override
    public long position () {
        return offset + ofNullable(buffer).map(Buffer::position).orElse(0);
    }

    @Override
    public boolean hasNext () {
        return ofNullable(buffer).map(Buffer::hasRemaining).orElse(false) ||
               currentFileSize() > position();
    }

    @Override
    public Optional<byte[]> next () {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return readInt().flatMap(this::read);
    }

    @Override
    @SneakyThrows
    public void position (long newPosition) {
        offset = newPosition;
        closeBuffer();
    }

    @Override
    public Path getPath () {
        return path;
    }

    @Override
    public void close () throws IOException {
        closeBuffer();
        channel.close();
    }

    @SneakyThrows
    private Optional<Integer> readInt () {
        return read(Integer.BYTES)
                .map(ByteBuffer::wrap)
                .map(ByteBuffer::getInt)
                .filter(it -> it > 0);
    }

    @SneakyThrows
    private Optional<byte[]> read (int length) {
        if (buffer == null || buffer.remaining() < length) {
            buffer = createMappedByteBuffer();
            if (buffer.remaining() < length) {
                return empty();
            }
        }

        val result = new byte[length];
        buffer.get(result);
        return of(result);
    }

    @SneakyThrows
    private MappedByteBuffer createMappedByteBuffer () {
        if (buffer != null) {
            offset = position();
            closeBuffer();
        }
        val limit = Math.min(bufferSize, Files.size(path) - offset);
        return channel.map(READ_ONLY, offset, limit);
    }

    private void closeBuffer () {
        if (buffer == null) {
            return;
        }

        getFieldValueFrom(buffer, "cleaner")
                .map(it -> invokeMethodOf(it, "clean"));

        buffer = null;
    }
}

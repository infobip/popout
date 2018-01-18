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

package org.infobip.lib.popout.writer;

import static java.nio.channels.FileChannel.MapMode.READ_WRITE;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.Optional.ofNullable;
import static lombok.AccessLevel.PRIVATE;
import static org.infobip.lib.popout.util.ReflectionUtils.getFieldValueFrom;
import static org.infobip.lib.popout.util.ReflectionUtils.invokeMethodOf;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

import lombok.Builder;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.val;

/**
 * Mmap implementation of {@link FileWriter} based on {@code MappedByteBuffer}.
 *
 * @since 1.0.0
 * @author Artem Labazin
 */
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class MmapFileWriter implements FileWriter {

    Path path;

    FileChannel channel;

    int bufferSize;

    @NonFinal
    MappedByteBuffer buffer;

    @NonFinal
    long offset;

    @NonFinal
    long limit;

    /**
     * Mmap write constructor.
     *
     * @param path       a file's path to write
     * @param bufferSize buffer's size in bytes. Default is 8192 bytes (8Kb)
     */
    @Builder
    @SneakyThrows
    public MmapFileWriter (@NonNull Path path, Integer bufferSize) {
        this.path = path;
        this.bufferSize = ofNullable(bufferSize).orElse(8192);

        channel = FileChannel.open(path, CREATE, READ, WRITE);
    }

    @Override
    @SneakyThrows
    public long currentFileSize () {
        trim();
        return channel.size();
    }

    @Override
    public long position () {
        return offset + ofNullable(buffer).map(Buffer::position).orElse(0);
    }

    @Override
    @SneakyThrows
    public void position (long newPosition) {
        offset = newPosition;
        closeBuffer();
        buffer = null;
    }

    @Override
    public Path getPath () {
        return path;
    }

    @Override
    public void write (byte[] bytes) {
        val toWrite = ByteBuffer.allocate(Integer.BYTES + bytes.length)
                .putInt(bytes.length)
                .put(bytes)
                .array();

        if (buffer == null || buffer.remaining() < toWrite.length) {
            buffer = createMappedByteBuffer();
        }
        buffer.put(toWrite);
        limit = Math.max(limit, position());
    }

    @Override
    @SneakyThrows
    public void trim () {
        offset = position();
        closeBuffer();
        channel.truncate(limit);
    }

    @Override
    public void close () throws IOException {
        trim();
        channel.close();
    }

    @SneakyThrows
    private MappedByteBuffer createMappedByteBuffer () {
        if (buffer != null) {
            offset = position();
            closeBuffer();
        }
        return channel.map(READ_WRITE, offset, bufferSize);
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

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

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import static lombok.AccessLevel.PRIVATE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.val;

/**
 * Channel implementation of {@link FileWriter} based on {@code FileChannel}.
 *
 * @since 1.0.0
 * @author Artem Labazin
 */
@FieldDefaults(level = PRIVATE, makeFinal = true)
public final class ChannelFileWriter implements FileWriter {

    Path path;

    FileChannel channel;

    ByteBuffer recordLengthBuffer;

    /**
     * Channel write constructor.
     *
     * @param path a file's path to write
     */
    @SneakyThrows
    public ChannelFileWriter (@NonNull Path path) {
        this.path = path;
        channel = FileChannel.open(path, CREATE, WRITE);
        recordLengthBuffer = ByteBuffer.allocate(Integer.BYTES);
    }

    @Override
    @SneakyThrows
    public long currentFileSize () {
        return channel.size();
    }

    @Override
    @SneakyThrows
    public long position () {
        return channel.position();
    }

    @Override
    @SneakyThrows
    public void position (long offset) {
        channel.position(offset);
    }

    @Override
    public Path getPath () {
        return path;
    }

    @Override
    @SneakyThrows
    public void write (byte[] bytes) {
        recordLengthBuffer.clear();
        recordLengthBuffer.putInt(bytes.length);
        recordLengthBuffer.flip();

        channel.write(recordLengthBuffer);

        val buffer = ByteBuffer.allocate(bytes.length)
                .put(bytes);

        buffer.flip();
        channel.write(buffer);

        channel.force(true);
    }

    @Override
    public void trim () {
        // nothing
    }

    @Override
    public void close () throws IOException {
        channel.close();
    }
}

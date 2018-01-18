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

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static lombok.AccessLevel.PRIVATE;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Optional;

import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.val;

/**
 * Channel implementation of {@link FileReader} based on {@code FileChannel}.
 *
 * @since 1.0.0
 * @author Artem Labazin
 */
@FieldDefaults(level = PRIVATE, makeFinal = true)
public final class ChannelFileReader implements FileReader {

    Path path;

    FileChannel channel;

    ByteBuffer recordLengthBuffer;

    /**
     * Channel read constructor.
     *
     * @param path a file's path to read
     */
    @SneakyThrows
    public ChannelFileReader (@NonNull Path path) {
        this.path = path;
        channel = FileChannel.open(path, CREATE, READ);
        recordLengthBuffer = ByteBuffer.allocate(Integer.BYTES);
    }

    @Override
    @SneakyThrows
    public boolean hasNext () {
        return channel.size() > channel.position();
    }

    @Override
    public Optional<byte[]> next () {
        return readInt()
                .flatMap(this::read);
    }

    @Override
    public void close () throws IOException {
        channel.close();
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

    @SneakyThrows
    private Optional<Integer> readInt () {
        if (channel.read(recordLengthBuffer) == -1) {
            return empty();
        }

        recordLengthBuffer.flip();
        val length = recordLengthBuffer.getInt();
        recordLengthBuffer.clear();

        return of(length)
                .filter(it -> it > 0);
    }

    @SneakyThrows
    private Optional<byte[]> read (int length) {
        val buffer = ByteBuffer.allocate(length);
        if (channel.read(buffer) == -1) {
            return empty();
        }
        return of(buffer.array());
    }
}

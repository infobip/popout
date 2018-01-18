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

package org.infobip.lib.popout;

import static java.nio.channels.FileChannel.MapMode.READ_WRITE;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;
import static lombok.AccessLevel.PRIVATE;
import static org.infobip.lib.popout.util.ReflectionUtils.getFieldValueFrom;
import static org.infobip.lib.popout.util.ReflectionUtils.invokeMethodOf;

import java.io.Closeable;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;

import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.val;

/**
 * Metadata file object.
 *
 * @since 1.0.0
 * @author Artem Labazin
 */
@FieldDefaults(level = PRIVATE, makeFinal = true)
public final class MetadataFile implements Closeable {

    private static final short OFFSET_ELEMENTS = 0;

    private static final short OFFSET_HEAD = 8;

    private static final short OFFSET_TAIL = 20;

    MappedByteBuffer buffer;

    @Getter
    Position head;

    @Getter
    Position tail;

    @Getter
    @NonFinal
    long elements;

    /**
     * Constructor.
     *
     * @param path metadata file's path
     */
    @SneakyThrows
    public MetadataFile (@NonNull Path path) {
        val channel = FileChannel.open(path, CREATE, READ, WRITE);
        buffer = channel.map(READ_WRITE, 0, 32);

        head = new Position(OFFSET_HEAD);
        tail = new Position(OFFSET_TAIL);
    }

    /**
     * Moves queue's head.
     *
     * @param index  file's index
     * @param offset file's offset
     */
    public void moveHead (int index, long offset) {
        if (elements == 0) {
            throw new IllegalArgumentException("Queue is empty");
        } else if (index > tail.getIndex()) {
            val message = "New head index " + index + " is greater than tail's index (" + tail.getIndex() + ')';
            throw new IllegalArgumentException(message);
        } else if (index == tail.getIndex() && offset > tail.getOffset()) {
            val message = "New head offset " + offset + " is greater than tail's in the same index " + index;
            throw new IllegalArgumentException(message);
        }

        elements--;
        buffer.putLong(OFFSET_ELEMENTS, elements);

        head.move(index, offset);

        buffer.force();
    }

    /**
     * Moves queue's tail.
     *
     * @param index  file's index
     * @param offset file's offset
     */
    public void moveTail (int index, long offset) {
        if (index < head.getIndex()) {
            val message = "New tail index " + index + " is lower than head's index (" + head.getIndex() + ')';
            throw new IllegalArgumentException(message);
        } else if (index == head.getIndex() && offset < head.getOffset()) {
            val message = "New tail offset " + offset + " is lower than head's in the same index " + index;
            throw new IllegalArgumentException(message);
        }

        elements++;
        buffer.putLong(OFFSET_ELEMENTS, elements);

        tail.move(index, offset);

        buffer.force();
    }

    @Override
    public void close () throws IOException {
        buffer.force();
        getFieldValueFrom(buffer, "cleaner")
                .map(it -> invokeMethodOf(it, "clean"));
    }

    /**
     * Head or tail position.
     */
    public final class Position {

        final short writeOffset;

        @Getter
        int index;

        @Getter
        long offset;

        Position (short writeOffset) {
            this.writeOffset = writeOffset;
            index = buffer.getInt(writeOffset);
            offset = buffer.getLong(writeOffset + Integer.BYTES);
        }

        void move (int newIndex, long newOffset) {
            index = newIndex;
            offset = newOffset;

            buffer.putInt(writeOffset, index);
            buffer.putLong(writeOffset + Integer.BYTES, offset);
        }
    }
}

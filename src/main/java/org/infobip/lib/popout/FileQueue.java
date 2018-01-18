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

import static lombok.AccessLevel.PACKAGE;
import static lombok.AccessLevel.PRIVATE;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;

import org.infobip.lib.popout.reader.FolderReader;
import org.infobip.lib.popout.writer.FolderWriter;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;

/**
 * File-based {@link Queue} implementation.
 *
 * @see Queue
 * @since 1.0.0
 * @author Artem Labazin
 */
@RequiredArgsConstructor(access = PACKAGE)
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class FileQueue<T> extends AbstractQueue<T> implements Closeable {

    public static <V> FileQueueBuilder<V> in (String folder) {
        val path = Paths.get(folder);
        return in(path);
    }

    public static <V> FileQueueBuilder<V> in (Path folder) {
        return new FileQueueBuilder<>(folder);
    }

    @NonNull
    MetadataFile metadata;

    @NonNull
    FolderWriter<T> writer;

    @NonNull
    FolderReader<T> reader;

    @Override
    public Iterator<T> iterator () {
        return new QueueIterator();
    }

    @Override
    public int size () {
        return (int) longSize();
    }

    /**
     * Returns the number of elements in this collection.
     *
     * @return the number of elements in this collection
     */
    public long longSize () {
        return metadata.getElements();
    }

    @Override
    public boolean offer (@NonNull T element) {
        writer.writeNext(element);
        metadata.moveTail(
                writer.getCurrentFileIndex(),
                writer.getCurrentPosition()
        );
        return true;
    }

    @Override
    public T poll () {
        T result = reader.readNext().orElse(null);
        if (result != null) {
            metadata.moveHead(
                    reader.getCurrentFileIndex(),
                    reader.getCurrentPosition()
            );
        }
        return result;
    }

    @Override
    public T peek () {
        return reader.peek().orElse(null);
    }

    @Override
    public void close () throws IOException {
        metadata.close();
        writer.close();
        reader.close();
    }

    private class QueueIterator implements Iterator<T> {

        T nextItem;

        QueueIterator () {
            nextItem = poll();
        }

        @Override
        public boolean hasNext () {
            return nextItem != null;
        }

        @Override
        public T next () {
            T result = nextItem;
            if (result == null) {
                throw new NoSuchElementException();
            }
            nextItem = poll();
            return result;
        }
    }
}

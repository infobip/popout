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

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static lombok.AccessLevel.PRIVATE;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.infobip.lib.popout.MetadataFile.Position;
import org.infobip.lib.popout.util.FilePattern;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.val;

/**
 * Queue files reader in specified folder.
 *
 * @since 1.0.0
 * @author Artem Labazin
 * @param <T> the result type of method {@code readNext} or {@code peek}
 */
@Builder
@FieldDefaults(level = PRIVATE, makeFinal = true)
public final class FolderReader<T> implements Closeable {

    @NonNull
    Path folder;

    @NonNull
    FilePattern filePattern;

    @NonNull
    Function<Path, FileReader> createReader;

    @NonNull
    Deserializer<T> deserializer;

    @Getter(lazy = true, value = PRIVATE)
    AtomicInteger lazyCounter = initLazyCounter();

    @NonFinal
    FileReader currentFileReader;

    @NonFinal
    T peek;

    /**
     * Initialize folder reader with specified position (file's index and its offset).
     *
     * @param position a position to read
     *
     * @return this reader
     */
    public FolderReader<T> init (@NonNull Position position) {
        getExistingFile(position.getIndex())
                .map(createReader::apply)
                .ifPresent(it -> {
                    currentFileReader = it;
                    currentFileReader.position(position.getOffset());
                    getLazyCounter().set(position.getIndex() + 1);
                });

        return this;
    }

    /**
     * Returns next record from a queue file.
     *
     * @return optional record from file if it exists
     */
    public Optional<T> readNext () {
        try {
            return peek();
        } finally {
            peek = null;
            checkCurrentFileReader();
        }
    }

    /**
     * Returns peek record from a queue file.
     *
     * @return optional record from file if it exists
     */
    @SneakyThrows
    public Optional<T> peek () {
        if (peek != null) {
            return of(peek);
        }

        checkCurrentFileReader();
        if (currentFileReader == null) {
            return empty();
        }

        return currentFileReader.next()
                .map(deserializer::deserialize)
                .map(it -> peek = it);
    }

    /**
     * Returns current queue file's index.
     *
     * @return current file's index
     */
    public int getCurrentFileIndex () {
        int fileIndex = getLazyCounter().get() - 1;
        return Math.max(fileIndex, 0);
    }

    /**
     * Returns current position in reading file.
     *
     * @return current position
     */
    public long getCurrentPosition () {
        return ofNullable(currentFileReader)
                .map(FileReader::position)
                .orElse(0L);
    }

    @Override
    public void close () throws IOException {
        if (currentFileReader != null) {
            currentFileReader.close();
        }
    }

    @SneakyThrows
    private void checkCurrentFileReader () {
        if (currentFileReader != null && currentFileReader.hasNext()) {
            return;
        }

        if (currentFileReader != null) {
            currentFileReader.close();
            Files.deleteIfExists(currentFileReader.getPath());
        }

        currentFileReader = nextFile()
                .map(createReader::apply)
                .orElse(null);
    }

    @SneakyThrows
    private Optional<Path> nextFile () {
        val index = getLazyCounter().getAndIncrement();
        val optional = getExistingFile(index);
        if (optional.isPresent()) {
            return optional;
        }

        int newIndex = getLazyCounter()
                .updateAndGet(current -> tryToFindNextIndex().orElse(index));

        return getExistingFile(newIndex);
    }

    private Optional<Path> getExistingFile (int index) {
        val name = filePattern.getFileNameWith(index);
        return of(folder.resolve(name))
                .filter(Files::exists);
    }

    @SneakyThrows
    private AtomicInteger initLazyCounter () {
        return tryToFindNextIndex()
                .map(AtomicInteger::new)
                .orElse(new AtomicInteger(1));
    }

    @SneakyThrows
    private Optional<Integer> tryToFindNextIndex () {
        return Files.list(folder)
                .map(Path::getFileName)
                .map(Path::toString)
                .filter(filePattern::matches)
                .map(filePattern::extractIndex)
                .min(Integer::compare);
    }
}

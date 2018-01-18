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

import static java.util.Optional.ofNullable;
import static lombok.AccessLevel.PRIVATE;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
 * @param <T> the argument's type of method {@code writeNext}
 */
@Builder
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class FolderWriter<T> implements Closeable {

    @NonNull
    Path folder;

    @NonNull
    FilePattern filePattern;

    @NonNull
    Serializer<T> serializer;

    @NonNull
    Function<Path, FileWriter> createWriter;

    long maxFileSizeBytes;

    @Getter(lazy = true, value = PRIVATE)
    AtomicInteger lazyCounter = initLazyCounter();

    @NonFinal
    FileWriter currentFileWriter;

    /**
     * Initialize folder writer with specified position (file's index and its offset).
     *
     * @param position a position to write
     *
     * @return this reader
     */
    public FolderWriter<T> init (@NonNull Position position) {
        getLazyCounter().set(position.getIndex());

        val index = getLazyCounter().incrementAndGet();
        val name = filePattern.getFileNameWith(index);
        val path = folder.resolve(name);

        currentFileWriter = createWriter.apply(path);
        currentFileWriter.position(position.getOffset());

        return this;
    }

    /**
     * Writes a record to queue file.
     *
     * @param element record to write
     */
    @SneakyThrows
    public void writeNext (@NonNull T element) {
        val bytes = serializer.serialize(element);
        checkCurrentFileWriter();
        currentFileWriter.write(bytes);
    }

    /**
     * Returns current file's index.
     *
     * @return file's index
     */
    public int getCurrentFileIndex () {
        return getLazyCounter().get();
    }

    /**
     * Returns current writing file's position or 0 if there is no initialized {@link FileWriter}.
     *
     * @return current position
     */
    public long getCurrentPosition () {
        return ofNullable(currentFileWriter)
                .map(FileWriter::position)
                .orElse(0L);
    }

    @Override
    public void close () throws IOException {
        if (currentFileWriter != null) {
            currentFileWriter.close();
        }
    }

    @SneakyThrows
    private void checkCurrentFileWriter () {
        if (currentFileWriter != null && currentFileWriter.currentFileSize() < maxFileSizeBytes) {
            return;
        }

        if (currentFileWriter != null) {
            currentFileWriter.close();
        }

        val path = nextFile();
        currentFileWriter = createWriter.apply(path);
    }

    private Path nextFile () throws IOException {
        while (true) {
            val index = getLazyCounter().incrementAndGet();
            val name = filePattern.getFileNameWith(index);
            val path = folder.resolve(name);
            if (!Files.exists(path)) {
                return path;
            }
        }
    }

    @SneakyThrows
    private AtomicInteger initLazyCounter () {
        return Files.list(folder)
                .map(Path::getFileName)
                .map(Path::toString)
                .filter(filePattern::matches)
                .map(filePattern::extractIndex)
                .min(Integer::compare)
                .map(AtomicInteger::new)
                .orElse(new AtomicInteger(0));
    }
}

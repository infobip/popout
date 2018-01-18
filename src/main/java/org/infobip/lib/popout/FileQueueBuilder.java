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

import static java.util.Optional.ofNullable;
import static lombok.AccessLevel.PACKAGE;
import static lombok.AccessLevel.PRIVATE;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

import org.infobip.lib.popout.reader.ChannelFileReader;
import org.infobip.lib.popout.reader.DefaultDeserializer;
import org.infobip.lib.popout.reader.Deserializer;
import org.infobip.lib.popout.reader.FileReader;
import org.infobip.lib.popout.reader.FolderReader;
import org.infobip.lib.popout.reader.MmapFileReader;
import org.infobip.lib.popout.util.FilePattern;
import org.infobip.lib.popout.writer.ChannelFileWriter;
import org.infobip.lib.popout.writer.DefaultSerializer;
import org.infobip.lib.popout.writer.FileWriter;
import org.infobip.lib.popout.writer.FolderWriter;
import org.infobip.lib.popout.writer.MmapFileWriter;
import org.infobip.lib.popout.writer.Serializer;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;

/**
 * {@link FileQueue} builder. The only one way to instantiate {@link FileQueue}.
 *
 * @see FileQueue
 * @since 1.0.0
 * @author Artem Labazin
 */
@FieldDefaults(level = PRIVATE)
@RequiredArgsConstructor(access = PACKAGE)
public final class FileQueueBuilder<T> {

    final Path folder;

    Path metadataFile;

    FilePattern filePattern = new FilePattern("batch-", ".queue");

    long maxFileSizeBytes = (long) 100 * 1024 * 1024;

    boolean mmapEnabled;

    int mmapBufferSizeBytes = 8192;

    Serializer<T> serializer = new DefaultSerializer<>();

    Deserializer<T> deserializer = new DefaultDeserializer<>();

    /**
     * Sets path to metadata file, which contains information about the queue.
     *
     * @param metadataFilePath path to metadata file
     *
     * @return this builder
     */
    public FileQueueBuilder<T> metadataFile (@NonNull String metadataFilePath) {
        val path = Paths.get(metadataFilePath);
        return metadataFile(path);
    }

    /**
     * Sets path to metadata file, which contains information about the queue.
     *
     * @param path path to metadata file
     *
     * @return this builder
     */
    public FileQueueBuilder<T> metadataFile (@NonNull Path path) {
        metadataFile = path;
        return this;
    }

    /**
     * Sets queue files pattern.
     * <p>
     * Default is "batch-#.queue", where '#' sign is for file's index.
     *
     * @param pattern string queue's files pattern representation
     *
     * @return this builder
     */
    public FileQueueBuilder<T> filePattern (@NonNull String pattern) {
        return filePattern(FilePattern.from(pattern));
    }

    /**
     * Sets queue files pattern.
     * <p>
     * Default is "batch-#.queue", where '#' sign is for file's index.
     *
     * @param pattern queue's files pattern
     *
     * @return this builder
     */
    public FileQueueBuilder<T> filePattern (@NonNull FilePattern pattern) {
        filePattern = pattern;
        return this;
    }

    /**
     * Sets queue file's maximum size in <b>bytes</b>.
     * <p>
     * Default is 100Mb
     *
     * @param size the maximum size of queue file in <b>bytes</b>
     *
     * @return this builder
     */
    public FileQueueBuilder<T> maxFileSizeBytes (long size) {
        maxFileSizeBytes = size;
        return this;
    }

    /**
     * Enables using mmap file feature for speeding up write/read operations.
     * <p>
     * Default mmap file buffer size is 8Kb.
     *
     * @return this builder
     */
    public FileQueueBuilder<T> fileMmapEnable () {
        mmapEnabled = true;
        return this;
    }

    /**
     * Enables using mmap file feature for speeding up write/read operations.
     * <p>
     * Also it allows to setup mmap file buffer.
     * <p>
     * Default mmap file buffer size is 8Kb.
     *
     * @param bufferSizeBytes mmap file buffer size in <b>bytes</b>
     *
     * @return this builder
     */
    public FileQueueBuilder<T> fileMmapEnable (int bufferSizeBytes) {
        mmapBufferSizeBytes = bufferSizeBytes;
        return fileMmapEnable();
    }

    /**
     * Sets elements serializer, during write operations.
     * <p>
     * Default using implementation is {@link DefaultSerializer}
     *
     * @param elementsSerializer elements serializer
     *
     * @return this builder
     */
    public FileQueueBuilder<T> serializer (@NonNull Serializer<T> elementsSerializer) {
        serializer = elementsSerializer;
        return this;
    }

    /**
     * Sets elements deserializer, during read operations.
     * <p>
     * Default using implementation is {@link DefaultDeserializer}
     *
     * @param elementsDeserializer elements deserializer
     *
     * @return this builder
     */
    public FileQueueBuilder<T> deserializer (@NonNull Deserializer<T> elementsDeserializer) {
        deserializer = elementsDeserializer;
        return this;
    }

    /**
     * Builds {@link FileQueue} instance.
     *
     * @return {@link FileQueue} instance
     */
    public FileQueue<T> build () {
        val metadataFilePath = ofNullable(metadataFile)
                .map(folder::resolve)
                .orElseGet(() -> folder.resolve("queue.metadata"));

        val metadata = new MetadataFile(metadataFilePath);

        Function<Path, FileReader> createReader;
        Function<Path, FileWriter> createWriter;
        if (mmapEnabled) {
            createReader = path -> new MmapFileReader(path, mmapBufferSizeBytes);
            createWriter = path -> new MmapFileWriter(path, mmapBufferSizeBytes);
        } else {
            createReader = path -> new ChannelFileReader(path);
            createWriter = path -> new ChannelFileWriter(path);
        }

        val folderReader = FolderReader.<T>builder()
                .folder(folder)
                .filePattern(filePattern)
                .deserializer(deserializer)
                .createReader(createReader)
                .build()
                .init(metadata.getHead());

        val folderWriter = FolderWriter.<T>builder()
                .folder(folder)
                .filePattern(filePattern)
                .serializer(serializer)
                .createWriter(createWriter)
                .maxFileSizeBytes(maxFileSizeBytes)
                .build()
                .init(metadata.getTail());

        return new FileQueue<>(metadata, folderWriter, folderReader);
    }
}

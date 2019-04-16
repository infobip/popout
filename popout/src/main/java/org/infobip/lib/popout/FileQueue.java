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

package org.infobip.lib.popout;

import static java.util.Optional.ofNullable;
import static lombok.AccessLevel.PROTECTED;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractQueue;

import org.infobip.lib.popout.Deserializer.DefaultDeserializer;
import org.infobip.lib.popout.Serializer.DefaultSerializer;
import org.infobip.lib.popout.batched.BatchedFileQueueBuilder;
import org.infobip.lib.popout.synced.SyncedFileQueueBuilder;

import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.val;

/**
 * File-based {@link java.util.Queue} implementation.
 *
 * @param <T> the type of elements in this queue
 *
 * @since 1.0.0
 * @author Artem Labazin
 */
public abstract class FileQueue<T> extends AbstractQueue<T> implements AutoCloseable {

  /**
   * Start creating <b>synced</b> queue implementation.
   *
   * @param <T> the type of elements in this queue
   *
   * @return a queue builder object
   */
  public static <T> SyncedFileQueueBuilder<T> synced () {
    return new SyncedFileQueueBuilder<>();
  }

  /**
   * Start creating <b>batched</b> queue implementation.
   *
   * @param <T> the type of elements in this queue
   *
   * @return a queue builder object
   */
  public static <T> BatchedFileQueueBuilder<T> batched () {
    return new BatchedFileQueueBuilder<>();
  }

  /**
   * Returns the number of elements in this collection.
   *
   * @return the number of elements in this collection
   */
  public abstract long longSize ();

  /**
   * Returns the file system backed size occupied by the queue.
   *
   * @return the queue size (in bytes) on a disk
   */
  public abstract long diskSize ();

  /**
   * Flushes all this queue's data to the disk.
   */
  public abstract void flush ();

  @Override
  public abstract void close ();

  /**
   * The abstract file-based queue builder with common options and parameters
   * for all such queues implementations.
   *
   * @param <SELF> the type of builder's chain call return type
   *
   * @param <T> the type of elements in this builded queues
   *
   * @author Artem Labazin
   * @since 2.0.0
   */
  @Getter
  @SuppressWarnings({
      "unchecked",
      "checkstyle:ClassTypeParameterName"
  })
  @FieldDefaults(level = PROTECTED)
  public abstract static class Builder<SELF extends FileQueue.Builder<SELF, T>, T> {

    String name;

    Path folder;

    Serializer<T> serializer;

    Deserializer<T> deserializer;

    QueueLimit<T> limit;

    WalFilesConfig walFilesConfig;

    CompressedFilesConfig compressedFilesConfig;

    boolean restoreFromDisk = true;

    /**
     * Sets the queue's name. It uses in files names patters.
     *
     * @param value the new value
     *
     * @return this queue builder, for chain calls
     */
    public SELF name (@NonNull String value) {
      name = value;
      return (SELF) this;
    }

    /**
     * Sets a queue's folder path, which the queue uses
     * if the WAL and compressed configurations doesn't tell another.
     *
     * @param value the new value
     *
     * @return this queue builder, for chain calls
     */
    public SELF folder (@NonNull String value) {
      folder = Paths.get(value);
      return (SELF) this;
    }

    /**
     * Sets a queue's folder path, which the queue uses
     * if the WAL and compressed configurations doesn't tell another.
     *
     * @param value the new value
     *
     * @return this queue builder, for chain calls
     */
    public SELF folder (@NonNull Path value) {
      folder = value;
      return (SELF) this;
    }

    /**
     * Sets the {@link Serializer} implementation for queue elements.
     *
     * @param value the new value
     *
     * @return this queue builder, for chain calls
     */
    public SELF serializer (@NonNull Serializer<T> value) {
      serializer = value;
      return (SELF) this;
    }

    /**
     * Sets the {@link Deserializer} implementation for queue elements.
     *
     * @param value the new value
     *
     * @return this queue builder, for chain calls
     */
    public SELF deserializer (@NonNull Deserializer<T> value) {
      deserializer = value;
      return (SELF) this;
    }

    /**
     * Sets the queue limits.
     * <p>
     * The default value is an instance of {@link QueueLimit.NoLimit}.
     *
     * @param value the new value
     *
     * @return this queue builder, for chain calls
     */
    public SELF limit (@NonNull QueueLimit<T> value) {
      limit = value;
      return (SELF) this;
    }

    /**
     * Sets the WAL files configuration.
     * <p>
     * The default value has:
     * <ul>
     * <li>{@code 100} max WAL files amount, before compression</li>
     * <li>the folder from the main builder's folder value</li>
     * </ul>
     *
     * @param value the new value
     *
     * @return this queue builder, for chain calls
     */
    public SELF wal (@NonNull WalFilesConfig value) {
      walFilesConfig = value;
      return (SELF) this;
    }

    /**
     * Sets the compression files configuration.
     * <p>
     * The default value has:
     * <ul>
     * <li>{@code Long.MAX_VALUE} max file size (unlimited, another words)</li>
     * <li>the folder from the main builder's folder value</li>
     * </ul>
     *
     * @param value the new value
     *
     * @return this queue builder, for chain calls
     */
    public SELF compressed (@NonNull CompressedFilesConfig value) {
      compressedFilesConfig = value;
      return (SELF) this;
    }

    /**
     * Tells to the queue to restore its state from the previous run files or not.
     * <p>
     * If it is set to {@code false}, the queue, before start, removes all previous run files (if they exist).
     * <p>
     * If it is set to {@code true}, the queue re-reads all the files and restores its state.
     * <p>
     * The default value is: {@code true}
     *
     * @param value the new value
     *
     * @return this queue builder, for chain calls
     */
    public SELF restoreFromDisk (boolean value) {
      restoreFromDisk = value;
      return (SELF) this;
    }

    /**
     * Builds a new queue with parameters from the builder.
     *
     * @return a new queue
     */
    public FileQueue<T> build () {
      validateAndSetDefaults();
      return createQueue();
    }

    /**
     * The method for producing a new queue on each call based on builder's parameters.
     *
     * @return implementation-specific queue instance
     */
    protected abstract FileQueue<T> createQueue ();

    /**
     * The method for builder's parameters validation and setting the default values,
     * before creating a new queue.
     */
    @SneakyThrows
    protected void validateAndSetDefaults () {
      name = ofNullable(name)
          .orElse("queue");

      folder = ofNullable(folder)
          .orElse(Paths.get("."));

      serializer = ofNullable(serializer)
          .orElseGet(() -> new DefaultSerializer<T>());

      deserializer = ofNullable(deserializer)
          .orElseGet(() -> new DefaultDeserializer<T>());

      limit = ofNullable(limit)
          .orElseGet(() -> QueueLimit.noLimit());

      walFilesConfig = ofNullable(walFilesConfig)
          .map(it -> {
            if (it.getMaxCount() < 0) {
              val msg = "WAL's max count should be greater than 0, " +
                        "or 0 - if you would like to get default value (100)";
              throw new IllegalArgumentException(msg);
            }
            return it;
          })
          .map(it -> it.getFolder() == null
                     ? it.withFolder(folder)
                     : it
          )
          .map(it -> it.getMaxCount() == 0
                     ? it.withMaxCount(100)
                     : it
          )
          .orElseGet(() -> WalFilesConfig.builder()
              .folder(folder)
              .maxCount(100)
              .build());

      Files.createDirectories(walFilesConfig.getFolder());

      compressedFilesConfig = ofNullable(compressedFilesConfig)
          .map(it -> {
            if (it.getMaxSizeBytes() < 0) {
              val msg = "Compressed file's max size should be greater than 0, " +
                        "or 0 - if you would like to get default value (Long.MAX_VALUE)";
              throw new IllegalArgumentException(msg);
            }
            return it;
          })
          .map(it -> it.getFolder() == null
                     ? it.withFolder(folder)
                     : it
          )
          .map(it -> it.getMaxSizeBytes() == 0
                     ? it.withMaxSizeBytes(Long.MAX_VALUE)
                     : it
          )
          .orElseGet(() -> CompressedFilesConfig.builder()
              .folder(folder)
              .maxSizeBytes(Long.MAX_VALUE)
              .build());

      Files.createDirectories(compressedFilesConfig.getFolder());
    }
  }
}

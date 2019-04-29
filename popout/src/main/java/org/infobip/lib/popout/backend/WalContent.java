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

package org.infobip.lib.popout.backend;

import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

import java.nio.channels.FileChannel;
import java.nio.file.Path;

import lombok.Builder;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.val;

/**
 * WAL file's content descriptor.
 * <p>
 * The object could represent a whole file on the disk (WAL file)
 * or just a part of biger file (compressed file) with specific
 * {@code offset} and {@code length} values within that file.
 *
 * @since 2.0.0
 * @author Artem Labazin
 */
@Value
@Builder
public class WalContent {

  Path file;

  long offset;

  int length;

  /**
   * The method for safe opening the resource (WAL-file or part of compressed file) and
   * invoking the user specific action within it.
   *
   * @param consumer the action what to do in the open WAL content.
   */
  @SneakyThrows
  public void open (WalContentConsumer consumer) {
    try (val channel = FileChannel.open(file, READ, WRITE)) {
      channel.position(offset);
      consumer.accept(length, channel);
    }
  }

  /**
   * An operation that accepts the {@code length} and {@code channel} and returns no result.
   */
  public interface WalContentConsumer {

    /**
     * Performs this operation on the given arguments.
     *
     * @param length the total length of the WAL content
     *
     * @param channel the {@link FileChannel} instance for reading the WAL content
     */
    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    void accept (Integer length, FileChannel channel) throws Exception;
  }
}

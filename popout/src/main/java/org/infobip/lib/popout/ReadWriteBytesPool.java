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

import static lombok.AccessLevel.PRIVATE;

import io.appulse.utils.Bytes;
import io.appulse.utils.BytesPool;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.val;

/**
 * Singleton pool for read/write bytes buffers.
 *
 * @since 2.0.1
 * @author Artem Labazin
 */
@FieldDefaults(level = PRIVATE, makeFinal = true)
public final class ReadWriteBytesPool {

  /**
   * Returns the singleton instance of the pool.
   *
   * @return the singleton read/write pool instance
   */
  public static ReadWriteBytesPool getInstance () {
    return ReadWriteBytesPoolHolder.HOLDER_INSTANCE;
  }

  BytesPool pool;

  private ReadWriteBytesPool () {
    pool = BytesPool.builder()
        .initialBufferSizeBytes(512)
        .initialBuffersCount(3)
        .maximumBuffersCount(1024)
        .bufferCreateFunction(Bytes::resizableArray)
        .build();
  }

  /**
   * Borrows a byte buffer from the pool and sends it to the consumer.
   *
   * @param consumer acquired buffer consumer.
   */
  @SneakyThrows
  public <T> T borrow (@NonNull ThrowableBytesConsumer<T> consumer) {
    val buffer = pool.acquire();
    try {
      return consumer.consume(buffer);
    } finally {
      buffer.release();
    }
  }

  /**
   * The interface for propper bytes buffer consuming.
   */
  @FunctionalInterface
  public interface ThrowableBytesConsumer<T> {

    /**
     * Consumes the pooled buffer.
     *
     * @param buffer the pooled buffer
     *
     * @throws Throwable in case of any unexpected error
     */
    @SuppressWarnings("checkstyle:IllegalThrows")
    T consume (Bytes buffer) throws Throwable;
  }

  @SuppressWarnings("PMD.AccessorClassGeneration")
  private static class ReadWriteBytesPoolHolder {

    private static final ReadWriteBytesPool HOLDER_INSTANCE = new ReadWriteBytesPool();
  }
}

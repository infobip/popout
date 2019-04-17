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

package org.infobip.lib.popout.batched;

import static lombok.AccessLevel.PRIVATE;

import org.infobip.lib.popout.FileQueue;

import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * The specific builder object for a batched queue instance.
 *
 * @param <T> the type of elements in this queue
 *
 * @author Artem Labazin
 * @since 2.0.1
 */
@Getter
@FieldDefaults(level = PRIVATE)
public class BatchedFileQueueBuilder<T> extends FileQueue.Builder<BatchedFileQueueBuilder<T>, T> {

  public static final int MEMORY_ELEMENTS_MIN = 1;

  long batchSize;

  /**
   * Sets the amount of queue's elements placed in one WAL file.
   *
   * @param value the new value
   *
   * @return this queue builder, for chain calls
   */
  public BatchedFileQueueBuilder<T> batchSize (int value) {
    batchSize = value;
    return this;
  }

  @Override
  protected FileQueue<T> createQueue () {
    return new BatchedFileQueue<>(this);
  }

  @Override
  protected void validateAndSetDefaults () {
    super.validateAndSetDefaults();
    if (batchSize <= MEMORY_ELEMENTS_MIN) {
      throw new IllegalArgumentException("batchSize - must be greater than 1");
    }
  }
}

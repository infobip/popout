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

import java.util.function.BiConsumer;

import org.infobip.lib.popout.QueueLimit.DiskSize.DiskSizeBuilder;
import org.infobip.lib.popout.QueueLimit.QueueLength.QueueLengthBuilder;

import lombok.Builder;
import lombok.NonNull;

public interface QueueLimit<T> {

  static <T> QueueLimit<T> noLimit () {
    return new NoLimit<>();
  }

  static <T> QueueLengthBuilder<T> queueLength () {
    return QueueLength.<T>builder();
  }

  static <T> DiskSizeBuilder<T> diskSize () {
    return DiskSize.<T>builder();
  }

  boolean isExceeded (FileQueue<T> queue);

  void handle (T value, FileQueue<T> queue);

  class NoLimit<T> implements QueueLimit<T> {

    @Override
    public boolean isExceeded (FileQueue<T> queue) {
      return false;
    }

    @Override
    public void handle (T value, FileQueue<T> queue) {
      throw new UnsupportedOperationException();
    }
  }

  class QueueLength<T> implements QueueLimit<T> {

    private final long length;

    private final BiConsumer<T, FileQueue<T>> handler;

    @Builder
    QueueLength (long length, BiConsumer<T, FileQueue<T>> handler) {
      this.length = length;
      this.handler = handler;
    }

    @Override
    public boolean isExceeded (@NonNull FileQueue<T> queue) {
      return queue.longSize() > length;
    }

    @Override
    public void handle (@NonNull T value, @NonNull FileQueue<T> queue) {
      handler.accept(value, queue);
    }
  }

  class DiskSize<T> implements QueueLimit<T> {

    private final long bytes;

    private final BiConsumer<T, FileQueue<T>> handler;

    @Builder
    DiskSize (long bytes, BiConsumer<T, FileQueue<T>> handler) {
      this.bytes = bytes;
      this.handler = handler;
    }

    @Override
    public boolean isExceeded (@NonNull FileQueue<T> queue) {
      return queue.diskSize() > bytes;
    }

    @Override
    public void handle (@NonNull T value, @NonNull FileQueue<T> queue) {
      handler.accept(value, queue);
    }
  }
}

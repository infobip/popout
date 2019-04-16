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

import java.nio.file.Files;
import java.nio.file.Path;

import io.appulse.utils.Bytes;
import io.appulse.utils.ReadBytesUtils;
import io.appulse.utils.WriteBytesUtils;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.val;

@FieldDefaults(level = PRIVATE, makeFinal = true)
class Backup<T> {

  Path tail;

  Path head;

  @Builder
  Backup (String queueName, Path folder) {
    tail = folder.resolve(queueName + ".tail");
    head = folder.resolve(queueName + ".head");
  }

  boolean hasHead () {
    return Files.exists(head);
  }

  int restoreHead (Bytes buffer) {
    return restore(head, buffer);
  }

  boolean hasTail () {
    return Files.exists(tail);
  }

  int restoreTail (Bytes buffer) {
    return restore(tail, buffer);
  }

  void backupHead (Bytes buffer) {
    backup(head, buffer);
  }

  void backupTail (Bytes buffer) {
    backup(tail, buffer);
  }

  @SneakyThrows
  private int restore (Path file, Bytes buffer) {
    if (!Files.exists(file)) {
      return 0;
    }
    val size = (int) Files.size(file);
    if (!buffer.isWritable(size)) {
      val newCapacity = buffer.writerIndex() + size;
      buffer.capacity(newCapacity);
    }
    val readed = ReadBytesUtils.read(file, buffer);
    Files.delete(file);
    return readed;
  }

  @SneakyThrows
  private void backup (Path file, Bytes buffer) {
    if (Files.notExists(file)) {
      Files.createFile(file);
    }
    WriteBytesUtils.write(file, buffer);
  }
}

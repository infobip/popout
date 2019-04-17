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

import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.infobip.lib.popout.IOUtils.FOLDER;
import static org.infobip.lib.popout.IOUtils.allFiles;
import static org.infobip.lib.popout.IOUtils.clearTestFiles;
import static org.infobip.lib.popout.IOUtils.contentOf;
import static org.infobip.lib.popout.IOUtils.file;

import java.util.UUID;

import org.infobip.lib.popout.Deserializer;
import org.infobip.lib.popout.FileQueue;
import org.infobip.lib.popout.Serializer;
import org.infobip.lib.popout.WalFilesConfig;

import io.appulse.utils.Bytes;
import io.appulse.utils.BytesUtils;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BatchedFileQueueTests {

  @BeforeEach
  void beforeEach () {
    clearTestFiles();
  }

  @AfterEach
  void afterEach () {
    clearTestFiles();
  }

  @Test
  void add () {
    val chars = UUID.randomUUID().toString().toCharArray();
    val queue = FileQueue.<Character>batched()
        .name("batched-queue-add")
        .restoreFromDisk(true)
        .folder(FOLDER)
        .serializer(Serializer.CHARACTER)
        .deserializer(Deserializer.CHARACTER)
        .wal(WalFilesConfig.builder()
            .maxCount(2)
            .build())
        .batchSize(3)
        .build();

    for (val character : chars) {
      queue.add(character);
    }
    assertThat(queue.size()).isEqualTo(chars.length);

    assertThat(allFiles()).containsExactlyInAnyOrder(
        file("batched-queue-add-0.compressed"),
        file("batched-queue-add-1.compressed"),
        file("batched-queue-add-2.compressed"),
        file("batched-queue-add-9.wal"),
        file("batched-queue-add-10.wal")
    );

    assertThat(contentOf("batched-queue-add-0.compressed")).containsExactly(Bytes.resizableArray()
        .write1B(1).write8B(22) // header RECORD:chunk_length
        .write4B(3) // queue length
        .write4B(2).writeNB(BytesUtils.toBytes(chars[0]))
        .write4B(2).writeNB(BytesUtils.toBytes(chars[1]))
        .write4B(2).writeNB(BytesUtils.toBytes(chars[2]))
        .write1B(1).write8B(22) // header RECORD:chunk_length
        .write4B(3) // queue length
        .write4B(2).writeNB(BytesUtils.toBytes(chars[3]))
        .write4B(2).writeNB(BytesUtils.toBytes(chars[4]))
        .write4B(2).writeNB(BytesUtils.toBytes(chars[5]))
        .write1B(1).write8B(22) // header RECORD:chunk_length
        .write4B(3) // queue length
        .write4B(2).writeNB(BytesUtils.toBytes(chars[6]))
        .write4B(2).writeNB(BytesUtils.toBytes(chars[7]))
        .write4B(2).writeNB(BytesUtils.toBytes(chars[8]))
        .write1B(4).write8B(0) // header END
        .arrayCopy());

    assertThat(contentOf("batched-queue-add-10.wal")).containsExactly(Bytes.resizableArray()
        .write4B(3) // queue length
        .write4B(2).writeNB(BytesUtils.toBytes(chars[30]))
        .write4B(2).writeNB(BytesUtils.toBytes(chars[31]))
        .write4B(2).writeNB(BytesUtils.toBytes(chars[32]))
        .arrayCopy());
  }

  @Test
  void poll () {
    val chars = UUID.randomUUID().toString().toCharArray();
    val queue = FileQueue.<Character>batched()
        .name("batched-queue-poll")
        .restoreFromDisk(true)
        .folder(FOLDER)
        .serializer(Serializer.CHARACTER)
        .deserializer(Deserializer.CHARACTER)
        .wal(WalFilesConfig.builder()
            .maxCount(2)
            .build())
        .batchSize(3)
        .build();

    for (val character : chars) {
      queue.add(character);
    }
    assertThat(queue.size()).isEqualTo(chars.length);

    val array = new char[chars.length];
    for (int i = 0; i < array.length; i++) {
      val character = queue.poll();
      array[i] = character;
    }

    assertThat(array).isEqualTo(chars);
    assertThat(queue.size()).isEqualTo(0);
  }

  @Test
  void iterator () {
    val string = UUID.randomUUID().toString();
    val queue = FileQueue.<Character>batched()
        .name("batched-queue-iterator")
        .restoreFromDisk(true)
        .folder(FOLDER)
        .serializer(Serializer.CHARACTER)
        .deserializer(Deserializer.CHARACTER)
        .wal(WalFilesConfig.builder()
            .maxCount(2)
            .build())
        .batchSize(3)
        .build();

    for (val character : string.toCharArray()) {
      queue.add(character);
    }

    val sb = new StringBuilder();
    val iterator = queue.iterator();
    while (iterator.hasNext()) {
      sb.append(iterator.next());
    }

    assertThat(sb.toString()).isEqualTo(string);
    assertThat(queue.stream().map(it -> it.toString()).collect(joining()))
        .isEqualTo(string);

    assertThat(queue.toArray()).isEqualTo(string.toCharArray());
    assertThat(queue.contains('ё')).isFalse();
    assertThat(queue.contains(string.toCharArray()[4])).isTrue();

    assertThat(queue.toArray(new Character[0])[8])
        .isEqualTo(string.toCharArray()[8]);
    assertThat(queue.remove(string.toCharArray()[8]))
        .isTrue();
    assertThat(queue.toArray(new Character[0])[8])
        .isNotEqualTo(string.toCharArray()[8]);

    queue.clear();
    assertThat(queue.isEmpty()).isTrue();
    assertThat(queue.poll()).isNull();
  }

  @Test
  void compress () {
    val builder = FileQueue.<Character>batched()
        .name("batched-queue-compress")
        .restoreFromDisk(true)
        .folder(FOLDER)
        .serializer(Serializer.CHARACTER)
        .deserializer(Deserializer.CHARACTER)
        .wal(WalFilesConfig.builder()
            .maxCount(Integer.MAX_VALUE)
            .build())
        .batchSize(5);

    val chars = UUID.randomUUID().toString().toCharArray();
    try (val queue = builder.build()) {
      for (val character : chars) {
        queue.add(character);
      }

      assertThat(allFiles()).containsExactlyInAnyOrder(
          file("batched-queue-compress-0.wal"),
          file("batched-queue-compress-1.wal"),
          file("batched-queue-compress-2.wal"),
          file("batched-queue-compress-3.wal"),
          file("batched-queue-compress-4.wal"),
          file("batched-queue-compress-5.wal"),
          file("batched-queue-compress-6.wal")
      );

      queue.compress();

      assertThat(allFiles()).containsExactlyInAnyOrder(
          file("batched-queue-compress-0.compressed")
      );

      for (int i = 0; i < chars.length; i++) {
        val item = queue.poll();
        assertThat(item).isEqualTo(chars[i]);
      }
    }
  }

  @Test
  void flush () {
    val builder = FileQueue.<Character>batched()
        .name("batched-queue-flush")
        .restoreFromDisk(true)
        .folder(FOLDER)
        .serializer(Serializer.CHARACTER)
        .deserializer(Deserializer.CHARACTER)
        .wal(WalFilesConfig.builder()
            .maxCount(4)
            .build())
        .batchSize(5);

    val chars = UUID.randomUUID().toString().toCharArray();
    try (val queue = builder.build()) {
      for (val character : chars) {
        queue.add(character);
      }

      assertThat(queue.size()).isEqualTo(chars.length);
    }
    assertThat(allFiles()).containsExactlyInAnyOrder(
        file("batched-queue-flush-0.compressed"),
        file("batched-queue-flush-5.wal"),
        file("batched-queue-flush-6.wal"),
        file("batched-queue-flush-7.wal")
    );

    try (val queue = builder.build()) {
      assertThat(queue.size()).isEqualTo(chars.length);
      assertThat(queue.peek()).isEqualTo(chars[0]);
    }
    assertThat(allFiles()).containsExactlyInAnyOrder(
        file("batched-queue-flush-0.compressed"),
        file("batched-queue-flush-5.wal"),
        file("batched-queue-flush-6.wal"),
        file("batched-queue-flush-7.wal")
    );
  }
}

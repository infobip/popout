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

package org.infobip.lib.popout.synced;

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

class SyncedFileQueueTests {

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
    val queue = FileQueue.<Character>synced()
        .name("synced-queue-add")
        .folder(FOLDER)
        .serializer(Serializer.CHARACTER)
        .deserializer(Deserializer.CHARACTER)
        .wal(WalFilesConfig.builder()
            .maxCount(7)
            .build())
        .build();

    for (val character : chars) {
      queue.add(character);
    }
    assertThat(queue.size()).isEqualTo(chars.length);

    assertThat(allFiles()).containsExactlyInAnyOrder(
        file("synced-queue-add-0.compressed"),
        file("synced-queue-add-1.compressed"),
        file("synced-queue-add-2.compressed"),
        file("synced-queue-add-3.compressed"),
        file("synced-queue-add-32.wal"),
        file("synced-queue-add-33.wal"),
        file("synced-queue-add-34.wal"),
        file("synced-queue-add-35.wal")
    );

    assertThat(contentOf("synced-queue-add-0.compressed")).containsExactly(Bytes.resizableArray()
        .write1B(1).write8B(6) // header RECORD:chunk_length
        .write4B(2).writeNB(BytesUtils.toBytes(chars[0]))
        .write1B(1).write8B(6) // header RECORD:chunk_length
        .write4B(2).writeNB(BytesUtils.toBytes(chars[1]))
        .write1B(1).write8B(6) // header RECORD:chunk_length
        .write4B(2).writeNB(BytesUtils.toBytes(chars[2]))
        .write1B(1).write8B(6) // header RECORD:chunk_length
        .write4B(2).writeNB(BytesUtils.toBytes(chars[3]))
        .write1B(1).write8B(6) // header RECORD:chunk_length
        .write4B(2).writeNB(BytesUtils.toBytes(chars[4]))
        .write1B(1).write8B(6) // header RECORD:chunk_length
        .write4B(2).writeNB(BytesUtils.toBytes(chars[5]))
        .write1B(1).write8B(6) // header RECORD:chunk_length
        .write4B(2).writeNB(BytesUtils.toBytes(chars[6]))
        .write1B(1).write8B(6) // header RECORD:chunk_length
        .write4B(2).writeNB(BytesUtils.toBytes(chars[7]))
        .write1B(4).write8B(0) // header END
        .arrayCopy());

    assertThat(contentOf("synced-queue-add-32.wal")).containsExactly(Bytes.resizableArray()
        .write4B(2).writeNB(BytesUtils.toBytes(chars[32]))
        .arrayCopy());

  }

  @Test
  void poll () {
    val chars = UUID.randomUUID().toString().toCharArray();
    val queue = FileQueue.<Character>synced()
        .name("synced-queue-poll")
        .folder(FOLDER)
        .serializer(Serializer.CHARACTER)
        .deserializer(Deserializer.CHARACTER)
        .wal(WalFilesConfig.builder()
            .maxCount(2)
            .build())
        .build();

    for (val character : chars) {
      queue.add(character);
    }
    assertThat(queue.size()).isEqualTo(chars.length);

    assertThat(allFiles()).containsOnly(
        file("synced-queue-poll-0.compressed"),
        file("synced-queue-poll-1.compressed"),
        file("synced-queue-poll-2.compressed"),
        file("synced-queue-poll-3.compressed"),
        file("synced-queue-poll-4.compressed"),
        file("synced-queue-poll-5.compressed"),
        file("synced-queue-poll-6.compressed"),
        file("synced-queue-poll-7.compressed"),
        file("synced-queue-poll-8.compressed"),
        file("synced-queue-poll-9.compressed"),
        file("synced-queue-poll-10.compressed"),
        file("synced-queue-poll-11.compressed")
    );

    val array = new char[chars.length];
    for (int i = 0; i < array.length; i++) {
      val character = queue.poll();
      array[i] = character;
    }

    assertThat(array).isEqualTo(chars);
    assertThat(queue.size()).isEqualTo(0);
    assertThat(allFiles()).isEmpty();
  }

  @Test
  void iterator () {
    val string = UUID.randomUUID().toString();
    val queue = FileQueue.<Character>synced()
        .name("synced-queue-iterator")
        .folder(FOLDER)
        .serializer(Serializer.CHARACTER)
        .deserializer(Deserializer.CHARACTER)
        .wal(WalFilesConfig.builder()
            .maxCount(2)
            .build())
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

    assertThat(queue.size()).isEqualTo(36);

    assertThat(queue.toArray(new Character[0])[8])
        .isEqualTo(string.toCharArray()[8]);
    assertThat(queue.remove(string.toCharArray()[8]))
        .isTrue();
    assertThat(queue.toArray(new Character[0])[8])
        .isNotEqualTo(string.toCharArray()[8]);

    assertThat(queue.size()).isEqualTo(35);

    queue.clear();
    assertThat(allFiles()).isEmpty();
    assertThat(queue.poll()).isNull();
    assertThat(queue.size()).isEqualTo(0);
    assertThat(queue.isEmpty()).isTrue();
  }
}

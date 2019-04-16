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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.infobip.lib.popout.IOUtils.FOLDER;
import static org.infobip.lib.popout.IOUtils.clearTestFiles;
import static org.infobip.lib.popout.IOUtils.contentOf;
import static org.infobip.lib.popout.IOUtils.createFile;
import static org.infobip.lib.popout.IOUtils.delete;
import static org.infobip.lib.popout.IOUtils.file;
import static org.infobip.lib.popout.IOUtils.fileExists;

import org.infobip.lib.popout.CompressedFilesConfig;

import io.appulse.utils.Bytes;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompressedFilesTests {

  FilesManager walFiles;

  CompressedFiles compressedFiles;

  @BeforeEach
  void beforeEach () {
    clearTestFiles();

    createFile("popa-3.wal", "o");
    createFile("README.md", "z");
    createFile("popa-0wal", "z");
    createFile("popa--1wal", "z");
    createFile("popa-1.wal", "p");
    createFile("popa-10.wal", "pa");

    walFiles = FilesManager.builder()
        .folder(FOLDER)
        .prefix("popa-")
        .suffix(".wal")
        .build();

    compressedFiles = CompressedFiles.builder()
        .queueName("popa")
        .restoreFromDisk(false)
        .config(CompressedFilesConfig.builder()
            .folder(FOLDER)
            .maxSizeBytes(29L)
            .build())
        .build();
  }

  @AfterEach
  void afterEach () {
    // clearTestFiles();
  }

  @Test
  void pollContentPart () throws Exception {
    val wals = walFiles.getFilesFromFileSystem();
    val result1 = compressedFiles.compress(wals);
    val result2 = compressedFiles.compress(result1.getRemaining());

    assertThat(result2.getRemaining()).isEmpty();

    assertThat(compressedFiles.getFiles()).containsExactly(
        file("popa-0.compressed"),
        file("popa-1.compressed")
    );
    assertThat(fileExists("popa-0.compressed")).isTrue();
    assertThat(fileExists("popa-1.compressed")).isTrue();

    val buffer = Bytes.allocate(4);

    int readed = compressedFiles.pollContentPart(buffer);
    assertThat(readed).isEqualTo(1);
    assertThat(buffer.arrayCopy()).isEqualTo("p".getBytes(UTF_8));
    assertThat(fileExists("popa-0.compressed")).isTrue();

    readed = compressedFiles.pollContentPart(buffer);
    assertThat(readed).isEqualTo(1);
    assertThat(buffer.arrayCopy()).isEqualTo("po".getBytes(UTF_8));
    assertThat(fileExists("popa-0.compressed")).isFalse();

    readed = compressedFiles.pollContentPart(buffer);
    assertThat(readed).isEqualTo(2);
    assertThat(buffer.arrayCopy()).isEqualTo("popa".getBytes(UTF_8));
    assertThat(fileExists("popa-1.compressed")).isFalse();

    buffer.reset();

    readed = compressedFiles.pollContentPart(buffer);
    assertThat(readed).isEqualTo(0);
    assertThat(buffer.arrayCopy()).isEmpty();
  }

  @Test
  void compress () throws Exception {
    val result1 = compressedFiles.compress(walFiles.getFilesFromFileSystem());

    assertThat(result1.getCompressed()).containsExactly(
        file("popa-1.wal"),
        file("popa-3.wal")
    );
    assertThat(result1.getRemaining()).containsExactly(
        file("popa-10.wal")
    );
    delete("popa-1.wal", "popa-3.wal");

    val result2 = compressedFiles.compress(walFiles.getFilesFromFileSystem());

    assertThat(result2.getCompressed()).containsExactly(
        file("popa-10.wal")
    );
    assertThat(result2.getRemaining())
        .isEmpty();
    delete("popa-10.wal");

    val files = compressedFiles.getFiles();
    assertThat(files).containsExactly(
        file("popa-0.compressed"),
        file("popa-1.compressed")
    );

    val bytes1 = Bytes.resizableArray()
        .write1B(1).write8B(1)
        .writeNB("p")
        .write1B(1).write8B(1)
        .writeNB("o")
        .write1B(4).write8B(0)
        .arrayCopy();

    assertThat(contentOf("popa-0.compressed"))
        .containsExactly(bytes1);

    val bytes2 = Bytes.resizableArray()
        .write1B(1).write8B(2)
        .writeNB("pa")
        .write1B(4).write8B(0)
        .arrayCopy();

    assertThat(contentOf("popa-1.compressed"))
        .containsExactly(bytes2);
  }

  @Test
  void diskSize () {
    assertThat(compressedFiles.diskSize()).isEqualTo(0);

    val result1 = compressedFiles.compress(walFiles.getFilesFromFileSystem());
    assertThat(compressedFiles.diskSize()).isEqualTo(29L);
    delete(result1.getCompressed());

    val result2 = compressedFiles.compress(walFiles.getFilesFromFileSystem());
    assertThat(compressedFiles.diskSize()).isEqualTo(49L);
    delete(result2.getCompressed());
  }

  @Test
  void iterator () {
    val result1 = compressedFiles.compress(walFiles.getFilesFromFileSystem());
    delete(result1.getCompressed());

    int count1 = 0;
    val iterator1 = compressedFiles.iterator();
    while (iterator1.hasNext()) {
      iterator1.next();
      count1++;
    }
    assertThat(count1).isEqualTo(2);

    val result2 = compressedFiles.compress(walFiles.getFilesFromFileSystem());
    delete(result2.getCompressed());

    int count2 = 0;
    val iterator2 = compressedFiles.iterator();
    while (iterator2.hasNext()) {
      iterator2.next();
      count2++;
    }
    assertThat(count2).isEqualTo(3);
  }
}

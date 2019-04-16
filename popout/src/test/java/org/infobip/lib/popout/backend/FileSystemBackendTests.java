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
import static org.infobip.lib.popout.IOUtils.fileExists;

import java.nio.file.Files;

import org.infobip.lib.popout.CompressedFilesConfig;
import org.infobip.lib.popout.WalFilesConfig;

import io.appulse.utils.Bytes;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FileSystemBackendTests {

  FileSystemBackend fs;

  @BeforeEach
  @SneakyThrows
  void beforeEach () {
    clearTestFiles();
    Files.createDirectories(FOLDER);

    fs = FileSystemBackend.builder()
        .queueName("popa")
        .restoreFromDisk(true)
        .walConfig(WalFilesConfig.builder()
            .folder(FOLDER)
            .maxCount(1)
            .build())
        .compressedConfig(CompressedFilesConfig.builder()
            .folder(FOLDER)
            .maxSizeBytes(29L)
            .build())
        .build();
  }

  @AfterEach
  @SneakyThrows
  void afterEach () {
    clearTestFiles();
  }

  @Test
  void write () {
    fs.write(Bytes.wrap("p".getBytes(UTF_8)));

    assertThat(fileExists("popa-0.wal")).isTrue();
    assertThat(fileExists("popa-1.wal")).isFalse();
    assertThat(fileExists("popa-0.compressed")).isFalse();

    fs.write(Bytes.wrap("o".getBytes(UTF_8)));

    assertThat(fileExists("popa-0.wal")).isFalse();
    assertThat(fileExists("popa-1.wal")).isFalse();
    assertThat(fileExists("popa-0.compressed")).isTrue();

    fs.write(Bytes.wrap("pa".getBytes(UTF_8)));

    assertThat(fileExists("popa-0.wal")).isFalse();
    assertThat(fileExists("popa-1.wal")).isFalse();
    assertThat(fileExists("popa-2.wal")).isTrue();
    assertThat(fileExists("popa-0.compressed")).isTrue();
    assertThat(fileExists("popa-1.compressed")).isFalse();
  }

  @Test
  void pollTo () {
    fs.write(Bytes.wrap("p".getBytes(UTF_8)));
    fs.write(Bytes.wrap("o".getBytes(UTF_8)));
    fs.write(Bytes.wrap("pa".getBytes(UTF_8)));

    assertThat(fileExists("popa-2.wal")).isTrue();
    assertThat(fileExists("popa-0.compressed")).isTrue();

    val buffer = Bytes.allocate(4);

    int readed = fs.pollTo(buffer);
    assertThat(readed).isEqualTo(1);
    assertThat(buffer.arrayCopy()).isEqualTo("p".getBytes(UTF_8));
    assertThat(fileExists("popa-2.wal")).isTrue();
    assertThat(fileExists("popa-0.compressed")).isTrue();

    readed = fs.pollTo(buffer);
    assertThat(readed).isEqualTo(1);
    assertThat(buffer.arrayCopy()).isEqualTo("po".getBytes(UTF_8));
    assertThat(fileExists("popa-2.wal")).isTrue();
    assertThat(fileExists("popa-0.compressed")).isFalse();

    readed = fs.pollTo(buffer);
    assertThat(readed).isEqualTo(2);
    assertThat(buffer.arrayCopy()).isEqualTo("popa".getBytes(UTF_8));
    assertThat(fileExists("popa-2.wal")).isFalse();
    assertThat(fileExists("popa-0.compressed")).isFalse();

    buffer.reset();

    readed = fs.pollTo(buffer);
    assertThat(readed).isEqualTo(0);
    assertThat(buffer.arrayCopy()).isEmpty();
  }
}

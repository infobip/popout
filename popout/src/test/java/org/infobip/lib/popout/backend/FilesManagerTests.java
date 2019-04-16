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

import static lombok.AccessLevel.PRIVATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.infobip.lib.popout.IOUtils.FOLDER;
import static org.infobip.lib.popout.IOUtils.clearTestFiles;
import static org.infobip.lib.popout.IOUtils.createFile;
import static org.infobip.lib.popout.IOUtils.delete;
import static org.infobip.lib.popout.IOUtils.file;

import lombok.experimental.FieldDefaults;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@FieldDefaults(level = PRIVATE)
class FilesManagerTests {

  FilesManager filesManager;

  @BeforeEach
  void beforeEach () {
    clearTestFiles();

    createFile("popa-3.wal");
    createFile("README.md");
    createFile("popa-0wal");
    createFile("popa--1wal");
    createFile("popa-1.wal");

    filesManager = FilesManager.builder()
        .folder(FOLDER)
        .prefix("popa-")
        .suffix(".wal")
        .build();
  }

  @AfterEach
  void afterEach () {
    clearTestFiles();
  }

  @Test
  void files () throws Exception {
    assertThat(filesManager.getFilesFromFileSystem()).containsExactly(
        file("popa-1.wal"),
        file("popa-3.wal")
    );

    createFile("popa-2.wal");
    assertThat(filesManager.getFilesFromFileSystem()).containsExactly(
        file("popa-1.wal"),
        file("popa-2.wal"),
        file("popa-3.wal")
    );

    createFile("popa-100.wal");
    assertThat(filesManager.getFilesFromFileSystem()).containsExactly(
        file("popa-1.wal"),
        file("popa-2.wal"),
        file("popa-3.wal"),
        file("popa-100.wal")
    );

    delete("popa-3.wal");
    assertThat(filesManager.getFilesFromFileSystem()).containsExactly(
        file("popa-1.wal"),
        file("popa-2.wal"),
        file("popa-100.wal")
    );
  }

  @Test
  void getIndex () {
    assertThat(filesManager.getIndex(file("popa-99.wal")))
        .isEqualTo(99);

    val someFile = file("some.txt");
    assertThatThrownBy(() -> filesManager.getIndex(someFile))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("File '%s' doesn't have index group", someFile.toString());
  }

  @Test
  void getFile () {
    assertThat(filesManager.getFile(271))
        .isEqualTo(file("popa-271.wal"));
  }

  @Test
  void findFile () {
    assertThat(filesManager.findFile(271))
        .isEmpty();

    assertThat(filesManager.findFile(3))
        .hasValue(file("popa-3.wal"));
  }

  @Test
  void createNext () {
    assertThat(filesManager.createNextFile())
        .isEqualTo(file("popa-4.wal"));

    assertThat(filesManager.createNextFile())
        .isEqualTo(file("popa-5.wal"));

    assertThat(filesManager.getFilesFromFileSystem()).containsExactly(
        file("popa-1.wal"),
        file("popa-3.wal"),
        file("popa-4.wal"),
        file("popa-5.wal")
    );

    val manager = FilesManager.builder()
        .folder(FOLDER)
        .build();

    assertThat(manager.createNextFile())
        .isEqualTo(file("0"));

    assertThat(manager.createNextFile())
        .isEqualTo(file("1"));

    assertThat(manager.getFilesFromFileSystem()).containsExactly(
        file("0"),
        file("1")
    );
  }

  @Test
  void poll () {
    assertThat(filesManager.poll())
        .isEqualTo(file("popa-1.wal"));

    assertThat(filesManager.poll())
        .isEqualTo(file("popa-3.wal"));

    assertThat(filesManager.poll())
        .isNull();
    assertThat(filesManager.poll())
        .isNull();

    assertThat(filesManager.createNextFile())
        .isEqualTo(file("popa-4.wal"));

    assertThat(filesManager.poll())
        .isEqualTo(file("popa-4.wal"));

    assertThat(filesManager.poll())
        .isNull();
  }

  @Test
  void clear () {
    filesManager.createNextFile();
    assertThat(filesManager.getFilesFromFileSystem()).containsExactly(
        file("popa-1.wal"),
        file("popa-3.wal"),
        file("popa-4.wal")
    );

    filesManager.clear();
    assertThat(filesManager.getFilesFromFileSystem()).isEmpty();
    assertThat(filesManager.getFilesFromQueue()).isEmpty();

    filesManager.createNextFile();
    assertThat(filesManager.getFilesFromFileSystem()).containsExactly(
        file("popa-0.wal")
    );
  }
}

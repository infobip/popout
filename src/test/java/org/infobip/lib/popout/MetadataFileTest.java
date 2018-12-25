/*
 * Copyright 2018 Infobip Ltd.
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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lombok.experimental.FieldDefaults;

@FieldDefaults(level = PRIVATE)
public class MetadataFileTest {

  private static final Path PATH = Paths.get("queue.metadata");

  MetadataFile metadata;

  @BeforeEach
  void before () throws IOException {
    Files.deleteIfExists(PATH);
    Files.createFile(PATH);
    metadata = new MetadataFile(PATH);
  }

  @AfterEach
  void after () throws IOException {
    metadata = null;
    Files.deleteIfExists(PATH);
  }

  @Test
  void empty () {
    assertThat(metadata.getElements()).isEqualTo(0);

    assertThat(metadata.getHead().getIndex()).isEqualTo(0);
    assertThat(metadata.getHead().getOffset()).isEqualTo(0);

    assertThat(metadata.getTail().getIndex()).isEqualTo(0);
    assertThat(metadata.getTail().getOffset()).isEqualTo(0);
  }

  @Test
  void moveTail () {
    metadata.moveTail(1, 42);

    assertThat(metadata.getTail().getIndex()).isEqualTo(1);
    assertThat(metadata.getTail().getOffset()).isEqualTo(42);

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> metadata.moveTail(-1, 0))
        .withMessage("New tail index -1 is lower than head's index (0)");

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> metadata.moveTail(0, -1))
        .withMessage("New tail offset -1 is lower than head's in the same index 0");
  }

  @Test
  void moveHead () {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> metadata.moveHead(0, 0))
        .withMessage("Queue is empty");

    metadata.moveTail(1, 42);

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> metadata.moveHead(2, 0))
        .withMessage("New head index 2 is greater than tail's index (1)");

    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> metadata.moveHead(1, 43))
        .withMessage("New head offset 43 is greater than tail's in the same index 1");

    metadata.moveHead(1, 11);

    assertThat(metadata.getHead().getIndex()).isEqualTo(1);
    assertThat(metadata.getHead().getOffset()).isEqualTo(11);
  }

  @Test
  void close () throws Exception {
    metadata.moveTail(2, 42);
    metadata.moveHead(1, 11);
    metadata.close();
    metadata = null;

    metadata = new MetadataFile(PATH);

    assertThat(metadata.getHead().getIndex()).isEqualTo(1);
    assertThat(metadata.getHead().getOffset()).isEqualTo(11);

    assertThat(metadata.getTail().getIndex()).isEqualTo(2);
    assertThat(metadata.getTail().getOffset()).isEqualTo(42);
  }
}

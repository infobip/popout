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

package org.infobip.lib.popout.reader;

import static java.nio.file.StandardOpenOption.APPEND;
import static lombok.AccessLevel.PRIVATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.val;

/**
 *
 * @author Artem Labazin
 */
// @Ignore
@FieldDefaults(level = PRIVATE)
@SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
abstract class AbstractFileReaderTest {

  private static final Path PATH = Paths.get("popa.txt");

  FileReader reader;

  @BeforeEach
  void before () throws IOException {
    Files.deleteIfExists(PATH);
    Files.createFile(PATH);
    reader = createFileReader(PATH);
  }

  @AfterEach
  void after () throws IOException {
    reader.close();
    Files.deleteIfExists(PATH);
  }

  @Test
  void readOne () {
    byte[] expected = write("Hello world");

    assertThat(reader.hasNext()).isTrue();
    assertThat(reader.next())
        .isPresent()
        .hasValue(expected);
  }

  @Test
  void readMany () {
    byte[] expected1 = write("one");
    byte[] expected2 = write("two");
    byte[] expected3 = write("three");

    assertThat(reader.hasNext()).isTrue();
    assertThat(reader.next())
        .isPresent()
        .hasValue(expected1);

    assertThat(reader.hasNext()).isTrue();
    assertThat(reader.next())
        .isPresent()
        .hasValue(expected2);

    assertThat(reader.hasNext()).isTrue();
    assertThat(reader.next())
        .isPresent()
        .hasValue(expected3);
  }

  @Test
  void emptyRead () {
    assertThat(reader.hasNext()).isFalse();
    assertThatThrownBy(() -> reader.next())
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void position () {
    assertThat(reader.position()).isEqualTo(0);

    byte[] expected1 = write("Hello world");
    byte[] expected2 = write("another record #1");
    byte[] expected3 = write("another record #2");

    assertThat(reader.next())
        .isPresent()
        .hasValue(expected1);

    assertThat(reader.position())
        .isEqualTo(Integer.BYTES + expected1.length);

    reader.position(reader.position() + Integer.BYTES + expected2.length);

    assertThat(reader.next())
        .isPresent()
        .hasValue(expected3);
  }

  @Test
  void currentFileSize () {
    assertThat(reader.currentFileSize()).isEqualTo(0);

    int length = write("one").length + Integer.BYTES;
    length += write("two").length + Integer.BYTES;
    length += write("three").length + Integer.BYTES;

    assertThat(reader.currentFileSize()).isEqualTo(length);
  }

  protected abstract FileReader createFileReader (Path path);

  @SneakyThrows
  private byte[] write (String string) {
    val bytes = string.getBytes();
    val record = ByteBuffer.allocate(Integer.BYTES + bytes.length)
        .putInt(bytes.length)
        .put(bytes)
        .array();

    Files.write(PATH, record, APPEND);
    return bytes;
  }
}

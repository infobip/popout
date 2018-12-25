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

package org.infobip.lib.popout.writer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PRIVATE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import lombok.experimental.FieldDefaults;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Artem Labazin
 */
// @Ignore
@FieldDefaults(level = PRIVATE)
abstract class AbstractFileWriterTest {

  private static final Path PATH = Paths.get("popa.txt");

  FileWriter writer;

  @BeforeEach
  public void before () throws IOException {
    Files.deleteIfExists(PATH);
    Files.createFile(PATH);
    writer = createFileWriter(PATH);
  }

  @AfterEach
  public void after () throws IOException {
    writer.close();
    Files.deleteIfExists(PATH);
  }

  @Test
  void writeNull () {
    assertThatThrownBy(() -> writer.write(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void writeEmpty () throws IOException {
    writer.write(new byte[0]);
    writer.trim();

    assertThat(Files.readAllBytes(PATH))
        .isEqualTo(ByteBuffer.allocate(Integer.BYTES)
            .putInt(0)
            .array());

    assertThat(writer.position()).isEqualTo(Integer.BYTES);
  }

  @Test
  void simpleWrite () throws IOException {
    val bytes = "Hello world".getBytes(UTF_8);
    val expected = ByteBuffer.allocate(Integer.BYTES + bytes.length)
        .putInt(bytes.length)
        .put(bytes)
        .array();

    writer.write(bytes);
    writer.trim();

    assertThat(Files.readAllBytes(PATH))
        .isEqualTo(expected);
  }

  @Test
  void multipleWrites () throws IOException {
    List<byte[]> records = toBytes("one", "two", "three");
    val expected = toRecordsBytes(records);

    records.forEach(writer::write);
    writer.trim();

    assertThat(Files.readAllBytes(PATH))
        .isEqualTo(expected);
  }

  @Test
  void position () throws IOException {
    assertThat(writer.position()).isEqualTo(0);

    List<byte[]> records = toBytes("one", "two", "three");
    val expected = toRecordsBytes(records);

    records.forEach(writer::write);

    assertThat(writer.position()).isEqualTo(expected.length);
    writer.position(Integer.BYTES + records.get(0).length);

    writer.write("one".getBytes(UTF_8));
    writer.trim();

    assertThat(Files.readAllBytes(PATH))
        .isEqualTo(toRecordsBytes("one", "one", "three"));
  }

  @Test
  void currentFileSize () throws IOException {
    assertThat(writer.currentFileSize()).isEqualTo(0);

    List<byte[]> records = toBytes("one", "two", "three");
    val expected = toRecordsBytes(records);

    records.forEach(writer::write);
    writer.trim();

    assertThat(writer.currentFileSize()).isEqualTo(expected.length);
  }

  protected abstract FileWriter createFileWriter (Path path);

  private List<byte[]> toBytes (String... records) {
    return Stream.of(records)
        .map(it -> it.getBytes(UTF_8))
        .collect(toList());
  }

  private byte[] toRecordsBytes (List<byte[]> records) {
    return records.stream()
        .map(it -> ByteBuffer.allocate(Integer.BYTES + it.length)
            .putInt(it.length)
            .put(it)
            .array()
        )
        .reduce(new byte[0], (a, b) -> {
            byte[] result = Arrays.copyOf(a, a.length + b.length);
            System.arraycopy(b, 0, result, a.length, b.length);
            return result;
        });
  }

  private byte[] toRecordsBytes (String... records) {
    return Stream.of(records)
        .map(it -> it.getBytes(UTF_8))
        .map(it -> ByteBuffer.allocate(Integer.BYTES + it.length)
            .putInt(it.length)
            .put(it)
            .array()
        )
        .reduce(new byte[0], (a, b) -> {
            byte[] result = Arrays.copyOf(a, a.length + b.length);
            System.arraycopy(b, 0, result, a.length, b.length);
            return result;
        });
  }
}

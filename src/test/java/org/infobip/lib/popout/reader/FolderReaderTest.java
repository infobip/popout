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

import static java.util.Arrays.asList;
import static lombok.AccessLevel.PRIVATE;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

import org.infobip.lib.popout.AbstractFolderBasedTest;
import org.infobip.lib.popout.MetadataFile;
import org.infobip.lib.popout.util.FilePattern;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.val;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Artem Labazin
 */
@FieldDefaults(level = PRIVATE)
@SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
public class FolderReaderTest extends AbstractFolderBasedTest {

    FolderReader<String> reader;

    @Before
    @Override
    public void before () throws IOException {
        super.before();
        reader = FolderReader.<String>builder()
                .folder(TEST_FOLDER)
                .filePattern(FilePattern.from("batch-#.queue"))
                .deserializer(new DefaultDeserializer<>())
                .createReader(path -> new ChannelFileReader(path))
                .build();
    }

    @After
    @Override
    public void after () throws IOException {
        reader = null;
        super.after();
    }

    @Test
    public void loadNonexistent () {
        assertThat(reader.readNext()).isNotPresent();

        writeTo("batch.queue");

        assertThat(reader.readNext()).isNotPresent();
    }

    @Test
    public void loadEmptyFile () throws IOException {
        Files.createFile(TEST_FOLDER.resolve("batch-1.queue"));

        assertThat(reader.readNext()).isNotPresent();
    }

    @Test
    public void simpleLoad () {
        writeTo("batch-1.queue").forEach(it -> {
            assertThat(reader.readNext())
                    .isPresent()
                    .hasValue(it);
        });

        assertThat(isFolderEmpty()).isTrue();
        assertThat(reader.readNext()).isNotPresent();
    }

    @Test
    public void loadNotFromFirstIndex () {
        writeTo("batch-42.queue").forEach(it -> {
            assertThat(reader.readNext())
                    .isPresent()
                    .hasValue(it);
        });

        assertThat(isFolderEmpty()).isTrue();
        assertThat(reader.readNext()).isNotPresent();
    }

    @Test
    public void multipleLoads () {
        val expected1 = writeTo("batch-1.queue");
        val expected2 = writeTo("batch-2.queue");
        val expected3 = writeTo("batch-42.queue");

        expected1.forEach(it -> {
            assertThat(reader.readNext())
                    .isPresent()
                    .hasValue(it);
        });
        assertThat(isFolderEmpty()).isFalse();

        expected2.forEach(it -> {
            assertThat(reader.readNext())
                    .isPresent()
                    .hasValue(it);
        });
        assertThat(isFolderEmpty()).isFalse();

        expected3.forEach(it -> {
            assertThat(reader.readNext())
                    .isPresent()
                    .hasValue(it);
        });
        assertThat(isFolderEmpty()).isTrue();
        assertThat(reader.readNext()).isNotPresent();
    }

    @Test
    public void loadFromFolderWithOtherFiles () throws IOException {
        Files.createFile(TEST_FOLDER.resolve("popa.txt"));
        assertThat(isFolderEmpty()).isFalse();

        writeTo("batch-1.queue").forEach(it -> {
            assertThat(reader.readNext())
                    .isPresent()
                    .hasValue(it);
        });
        assertThat(isFolderEmpty()).isFalse();
    }

    @Test
    public void peek () {
        assertThat(reader.peek()).isNotPresent();

        val expected = writeTo("batch-1.queue");

        assertThat(reader.peek())
                .isPresent()
                .hasValue(expected.get(0));

        assertThat(reader.peek())
                .isPresent()
                .hasValue(expected.get(0));

        expected.forEach(it -> {
            assertThat(reader.readNext())
                    .isPresent()
                    .hasValue(it);
        });

        assertThat(reader.peek()).isNotPresent();
    }

    @Test
    public void init () throws IOException {
        MetadataFile metadata = new MetadataFile(TEST_FOLDER.resolve("popa.txt"));
        reader.init(metadata.getHead());

        assertThat(reader.getCurrentFileIndex()).isEqualTo(0);
        assertThat(reader.getCurrentPosition()).isEqualTo(0);

        metadata.moveTail(2, 0);
        metadata.moveHead(1, 0);
        reader.init(metadata.getHead());

        assertThat(reader.getCurrentFileIndex()).isEqualTo(0);
        assertThat(reader.getCurrentPosition()).isEqualTo(0);

        val expected = writeTo("batch-1.queue");
        val newPosition = getRecordLength(expected.get(1));
        metadata.moveTail(2, 0);
        metadata.moveHead(1, newPosition); // skip first record
        reader.init(metadata.getHead());

        assertThat(reader.getCurrentFileIndex()).isEqualTo(1);
        assertThat(reader.getCurrentPosition()).isEqualTo(newPosition);

        assertThat(reader.readNext())
                .isPresent()
                .hasValue(expected.get(1));
        assertThat(reader.readNext())
                .isPresent()
                .hasValue(expected.get(2));

        metadata.close();
    }

    @SneakyThrows
    private List<String> writeTo (String fileName) {
        val items = asList(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString()
        );

        val path = TEST_FOLDER.resolve(fileName);

        @Cleanup
        OutputStream outputStream = Files.newOutputStream(path);

        for (String item : items) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(item);

            val bytes = byteArrayOutputStream.toByteArray();
            outputStream.write(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
            outputStream.write(bytes);
        }
        return items;
    }

    @SneakyThrows
    private long getRecordLength (String string) {
        val byteArrayOutputStream = new ByteArrayOutputStream();
        val objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(string);
        return Integer.BYTES + byteArrayOutputStream.toByteArray().length;
    }
}

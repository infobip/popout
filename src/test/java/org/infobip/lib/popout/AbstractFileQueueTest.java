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

import java.io.IOException;
import java.nio.file.Path;

import lombok.experimental.FieldDefaults;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 *
 * @author Artem Labazin
 */
@Ignore
@FieldDefaults(level = PRIVATE)
abstract class AbstractFileQueueTest extends AbstractFolderBasedTest {

    FileQueue<String> queue;

    @Before
    @Override
    public void before () throws IOException {
        super.before();
        queue = createFileQueue(TEST_FOLDER, "batch-#.queue", 50);
    }

    @After
    @Override
    public void after () throws IOException {
        queue.close();
        queue = null;
        super.after();
    }

    @Test(expected = NullPointerException.class)
    public void addNull () {
        queue.add(null);
    }

    @Test
    public void addSimple () {
        queue.add("Hello");
        queue.add("Hello");
        queue.add("Hello");

        assertThat(files()).containsExactly("batch-1.queue",
                                            "queue.metadata");
    }

    @Test
    public void addMany () throws InterruptedException {
        queue.add("Hello");
        queue.add("Hello");
        queue.add("Hello");
        queue.add("Hello");
        queue.add("Hello");
        queue.add("Hello");
        queue.add("Hello");
        queue.add("Hello");
        queue.add("Hello");

        assertThat(files()).containsExactly("batch-1.queue",
                                            "batch-2.queue",
                                            "batch-3.queue",
                                            "queue.metadata");
    }

    @Test
    public void pollEmpty () {
        assertThat(isFolderEmpty()).isFalse();
        assertThat(queue.poll()).isNull();
    }

    @Test
    public void pollOne () {
        queue.add("Hello");

        assertThat(isFolderEmpty()).isFalse();

        assertThat(queue.poll())
                .isNotNull()
                .isEqualTo("Hello");
    }

    @Test
    public void pollMany () throws InterruptedException {
        queue.add("one");
        queue.add("two");
        queue.add("three");

        assertThat(isFolderEmpty()).isFalse();

        assertThat(queue.poll())
                .isNotNull()
                .isEqualTo("one");

        assertThat(queue.poll())
                .isNotNull()
                .isEqualTo("two");

        assertThat(queue.poll())
                .isNotNull()
                .isEqualTo("three");
    }

    protected abstract FileQueue<String> createFileQueue (Path folder, String filePattern, int maxFileSize);
}

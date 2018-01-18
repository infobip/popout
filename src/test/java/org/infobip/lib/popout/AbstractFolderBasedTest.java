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

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;

/**
 *
 * @author Artem Labazin
 */
@SuppressWarnings("checkstyle:DesignForExtension")
public abstract class AbstractFolderBasedTest {

    protected static final Path TEST_FOLDER;

    static {
        TEST_FOLDER = Paths.get("./test_data");
    }

    @Before
    public void before () throws IOException {
        if (Files.isDirectory(TEST_FOLDER)) {
            cleanup();
        }
        Files.createDirectories(TEST_FOLDER);
    }

    @After
    public void after () throws IOException {
        cleanup();
    }

    @SneakyThrows
    protected boolean isFolderEmpty () {
        return Files.isDirectory(TEST_FOLDER) && files().isEmpty();
    }

    @SneakyThrows
    protected List<String> files () {
        return Files.list(TEST_FOLDER)
                .map(Path::getFileName)
                .map(Path::toString)
                .sorted()
                .collect(toList());
    }

    @SneakyThrows
    private void cleanup () {
        Files.walkFileTree(TEST_FOLDER, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult postVisitDirectory (Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return CONTINUE;
                }

                @Override
                public FileVisitResult visitFile (Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return CONTINUE;
                }
        });
    }
}

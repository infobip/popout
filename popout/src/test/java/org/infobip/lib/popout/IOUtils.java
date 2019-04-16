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

package org.infobip.lib.popout;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.Comparator.reverseOrder;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Collectors.toList;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import lombok.SneakyThrows;
import lombok.val;

@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
public final class IOUtils {

  public static final Path FOLDER = Paths.get("./test-files");

  @SneakyThrows
  public static void clearTestFiles () {
    if (Files.notExists(FOLDER)) {
      return;
    }

    Files.walk(FOLDER)
        .sorted(reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete);
  }

  @SneakyThrows
  public static List<Path> allFiles () {
    return Files.list(FOLDER).collect(toList());
  }

  public static Path file (String name) {
    return FOLDER.resolve(name);
  }

  public static boolean fileExists (String name) {
    val path = file(name);
    return Files.exists(path);
  }

  @SneakyThrows
  public static Path createFile (String name, String content) {
    Files.createDirectories(FOLDER);
    val path = FOLDER.resolve(name);
    Files.write(path, content.getBytes(UTF_8), CREATE_NEW, WRITE);
    return path;
  }

  @SneakyThrows
  public static Path createFile (String name) {
    Files.createDirectories(FOLDER);
    val path = FOLDER.resolve(name);
    Files.createFile(path);
    return path;
  }

  @SneakyThrows
  public static void delete (String... names) {
    for (val name : names) {
      val path = FOLDER.resolve(name);
      Files.delete(path);
    }
  }

  @SneakyThrows
  public static void delete (Path... paths) {
    for (val path : paths) {
      Files.delete(path);
    }
  }

  @SneakyThrows
  public static void delete (Collection<Path> paths) {
    for (val path : paths) {
      Files.delete(path);
    }
  }

  @SneakyThrows
  public static byte[] contentOf (String name) {
    return Files.readAllBytes(FOLDER.resolve(name));
  }

  @SneakyThrows
  public static void sleep (int seconds) {
    SECONDS.sleep(seconds);
  }

  private IOUtils () {
    throw new UnsupportedOperationException();
  }
}

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

package org.infobip.lib.popout.benchmarks;

import static org.openjdk.jmh.annotations.Mode.Throughput;

import static java.util.concurrent.TimeUnit.SECONDS;
import static lombok.AccessLevel.PRIVATE;
import static io.appulse.utils.SizeUnit.MEGABYTES;
import static java.util.Comparator.reverseOrder;
import static org.openjdk.jmh.annotations.Level.Iteration;
import static org.openjdk.jmh.annotations.Level.Trial;
import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.openjdk.jmh.annotations.Scope.Benchmark;

import java.io.File;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;

import org.infobip.lib.popout.CompressedFilesConfig;
import org.infobip.lib.popout.Deserializer;
import org.infobip.lib.popout.FileQueue;
import org.infobip.lib.popout.QueueLimit;
import org.infobip.lib.popout.Serializer;
import org.infobip.lib.popout.WalFilesConfig;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.infra.ThreadParams;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.val;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
// @Fork(2)
// @State(Benchmark)
// @OutputTimeUnit(SECONDS)
// @BenchmarkMode(Throughput)
// @Warmup(iterations = 1, time = 30, timeUnit = SECONDS)
// @Measurement(iterations = 3, time = 30, timeUnit = SECONDS)
public class BatchedReadBenchmarks {

  private static final Path TEST_FILES = Paths.get("./batched_files_for_read_tests");

  private static final Path FOLDER = Paths.get("./batched_benchmarks");

  Queue<byte[]> queue;

  // @Benchmark
  // @Threads(1)
  public void read_threads_1 (ThreadParams threadParams, Blackhole blackhole) {
    byte[] bytes = queue.poll();
    blackhole.consume(bytes);
  }

  // @Benchmark
  // @Threads(2)
  public void read_threads_2 (ThreadParams threadParams, Blackhole blackhole) {
    byte[] bytes = queue.poll();
    blackhole.consume(bytes);
  }

  // @Benchmark
  // @Threads(4)
  public void read_threads_4 (ThreadParams threadParams, Blackhole blackhole) {
    byte[] bytes = queue.poll();
    blackhole.consume(bytes);
  }

  // @Benchmark
  // @Threads(8)
  public void read_threads_8 (ThreadParams threadParams, Blackhole blackhole) {
    byte[] bytes = queue.poll();
    blackhole.consume(bytes);
  }

  // @Setup(Trial)
  public void beforeAll () {
    byte[] payload = new byte[512];
    ThreadLocalRandom.current().nextBytes(payload);

    val builder = FileQueue.<byte[]>batched()
        .name("batched-read")
        .folder(TEST_FILES)
        .serializer(Serializer.BYTE_ARRAY)
        .deserializer(Deserializer.BYTE_ARRAY)
        .limit(QueueLimit.noLimit())
        .restoreFromDisk(false)
        .wal(WalFilesConfig.builder()
            .folder(TEST_FILES)
            .maxCount(1000)
            .build())
        .compressed(CompressedFilesConfig.builder()
            .folder(TEST_FILES)
            .maxSizeBytes(MEGABYTES.toBytes(256))
            .build())
        .batchSize(10_000);

    log.info("Creating temporary files for READ benchmark tests");
    try (val tmpQueue = builder.build()) {
      for (int i = 0; i < 25_000_000; i++) {
        tmpQueue.add(payload);
      }
    }
    log.info("Creating files end");
  }

  @SneakyThrows
  // @Setup(Iteration)
  public void setup () {
    deleteFolder(FOLDER);
    Files.createDirectories(FOLDER);

    Files.walkFileTree(TEST_FILES, new CopyFilesVisitor(TEST_FILES, FOLDER));

    queue = FileQueue.<byte[]>batched()
        .name("batched-read")
        .folder(FOLDER)
        .serializer(Serializer.BYTE_ARRAY)
        .deserializer(Deserializer.BYTE_ARRAY)
        .limit(QueueLimit.noLimit())
        .restoreFromDisk(false)
        .wal(WalFilesConfig.builder()
            .folder(FOLDER)
            .maxCount(1000)
            .build())
        .compressed(CompressedFilesConfig.builder()
            .folder(FOLDER)
            .maxSizeBytes(MEGABYTES.toBytes(256))
            .build())
        .batchSize(10_000)
        .build();
  }

  // @TearDown(Iteration)
  public void tearDown () throws Exception {
    ((AutoCloseable) queue).close();
    deleteFolder(FOLDER);
  }

  // @TearDown(Trial)
  public void tearDownAll () {
    deleteFolder(TEST_FILES);
  }

  @SneakyThrows
  private void deleteFolder (Path path) {
    if (Files.notExists(path)) {
      return;
    }

    Files.walk(path)
        .sorted(reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete);
  }

  @AllArgsConstructor
  @FieldDefaults(level = PRIVATE)
  private class CopyFilesVisitor extends SimpleFileVisitor<Path> {

    Path source;

    Path destination;

    @Override
    @SneakyThrows
    public FileVisitResult preVisitDirectory (Path dir, BasicFileAttributes attrs) {
      val relativePath = source.relativize(dir);
      val targetDirPath = destination.resolve(relativePath);
      Files.copy(dir, targetDirPath, REPLACE_EXISTING);
      return CONTINUE;
    }

    @Override
    @SneakyThrows
    public FileVisitResult visitFile (Path file, BasicFileAttributes attrs) {
      val relativeFilePath = source.relativize(file);
      val targetFilePath = destination.resolve(relativeFilePath);
      Files.copy(file, targetFilePath, REPLACE_EXISTING);
      return CONTINUE;
    }
  }
}

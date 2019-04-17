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

import static org.openjdk.jmh.annotations.Mode.SingleShotTime;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static io.appulse.utils.SizeUnit.MEGABYTES;
import static java.util.Comparator.reverseOrder;
import static org.openjdk.jmh.annotations.Level.Iteration;
import static org.openjdk.jmh.annotations.Scope.Benchmark;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

import lombok.SneakyThrows;
import lombok.val;

@Fork(2)
@State(Benchmark)
@OutputTimeUnit(SECONDS)
@Warmup(iterations = 1)
@Measurement(iterations = 3)
@BenchmarkMode(SingleShotTime)
public class SyncedIteratorBenchmarks {

  private static final Path FOLDER = Paths.get("./synced_benchmarks");

  Queue<byte[]> queue;

  @Benchmark
  public void iterate (Blackhole blackhole) {
    val iterator = queue.iterator();
    while (iterator.hasNext()) {
      val item = iterator.next();
      blackhole.consume(item);
    }
  }

  @Setup(Iteration)
  @SneakyThrows
  public void setup () {
    deleteFolder(FOLDER);
    Files.createDirectories(FOLDER);

    queue = FileQueue.<byte[]>synced()
        .name("synced-iterator")
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
        .build();

    val payload = new byte[512];
    ThreadLocalRandom.current().nextBytes(payload);

    for (int i = 0; i < 1_000_000; i++) {
      queue.add(payload);
    }
  }

  @TearDown(Iteration)
  public void tearDown () throws Exception {
    ((AutoCloseable) queue).close();
    deleteFolder(FOLDER);
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
}

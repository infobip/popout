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
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.Comparator.reverseOrder;
import static org.openjdk.jmh.annotations.Level.Iteration;
import static org.openjdk.jmh.annotations.Scope.Benchmark;

import java.io.Closeable;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ThreadLocalRandom;

import com.squareup.tape2.QueueFile;
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
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.Value;
import lombok.val;

@Fork(2)
@State(Benchmark)
@OutputTimeUnit(SECONDS)
@BenchmarkMode(Throughput)
@Warmup(iterations = 1, time = 1, timeUnit = MINUTES)
@Measurement(iterations = 3, time = 1, timeUnit = MINUTES)
public class TapeWriteBenchmarks2 {

  private static final Path QUEUE_FILE = Paths.get("./tape_benchmarks/queue");

  SynchronizedQueueWrapper wrapper;

  byte[] payload;

  @Benchmark
  @Threads(1)
  public void write_threads_1 () {
    wrapper.add(payload);
  }

  @Benchmark
  @Threads(2)
  public void write_threads_2 () {
    wrapper.add(payload);
  }

  @Benchmark
  @Threads(8)
  public void write_threads_8 () {
    wrapper.add(payload);
  }

  @Setup(Iteration)
  @SneakyThrows
  public void setup () {
    deleteFolder(QUEUE_FILE);
    Files.createDirectories(QUEUE_FILE.getParent());

    QueueFile queue = new QueueFile
        .Builder(QUEUE_FILE.toFile())
        .build();

    wrapper = new SynchronizedQueueWrapper(queue);

    payload = new byte[512];
    ThreadLocalRandom.current().nextBytes(payload);
  }

  @TearDown(Iteration)
  public void tearDown () throws Exception {
    wrapper.close();
    deleteFolder(QUEUE_FILE);
  }

  @SneakyThrows
  private void deleteFolder (Path path) {
    if (Files.notExists(path)) {
      return;
    }

    val folder = path.getParent();
    Files.walk(folder)
        .sorted(reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete);
  }

  @Value
  public static class SynchronizedQueueWrapper implements Closeable {

    @NonNull
    QueueFile queue;

    @SneakyThrows
    public synchronized void add (byte[] bytes) {
      queue.add(bytes);
    }

    @Override
    @SneakyThrows
    public void close () {
      queue.close();
    }
  }
}

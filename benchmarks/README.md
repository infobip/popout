# Overview

## Benchmark host machine

For benchmarking we are using two different machines:

- small, but fast, `DigitalOcean`'s droplet with **SSD** drive;
- VPS with slow **HDD** disk.

## The results

### Write operations

- **JMH version:** 1.21
- **VM version:** JDK 1.8.0_212, Java HotSpot(TM) 64-Bit Server VM, 25.212-b10
- **VM options:** -Xms256m -Xmx1G
- **Warmup:** 1 iterations, 1 min each
- **Measurement:** 3 iterations, 1 min each
- **Timeout:** 10 min per iteration
- **Benchmark mode:** Throughput, ops/time

This benchamark show us a comparative write operations with different threads numbers (1/2/8).

**SSD** `DigitalOcean` droplet:

| Benchmark                                                                                | Threads | Mode  | Score       |      Error | Units |
|:-----------------------------------------------------------------------------------------|:-------:|:-----:|------------:|-----------:|:------|
| [Batched](./src/main/java/org/infobip/lib/popout/benchmarks/BatchedWriteBenchmarks.java) |    1    | thrpt |  483900.090 |  12183.906 | ops/s |
| [Batched](./src/main/java/org/infobip/lib/popout/benchmarks/BatchedWriteBenchmarks.java) |    2    | thrpt |  476730.573 |  27742.989 | ops/s |
| [Batched](./src/main/java/org/infobip/lib/popout/benchmarks/BatchedWriteBenchmarks.java) |    8    | thrpt |  230877.465 |   7111.418 | ops/s |
| [Synced](./src/main/java/org/infobip/lib/popout/benchmarks/SyncedWriteBenchmarks.java)   |    1    | thrpt |   12575.638 |    606.869 | ops/s |
| [Synced](./src/main/java/org/infobip/lib/popout/benchmarks/SyncedWriteBenchmarks.java)   |    2    | thrpt |   12117.054 |    449.219 | ops/s |
| [Synced](./src/main/java/org/infobip/lib/popout/benchmarks/SyncedWriteBenchmarks.java)   |    8    | thrpt |   11827.501 |   1100.236 | ops/s |
| [tape](./src/main/java/org/infobip/lib/popout/benchmarks/TapeWriteBenchmarks2.java)      |    1    | thrpt |    1716.828 |    120.102 | ops/s |
| [tape](./src/main/java/org/infobip/lib/popout/benchmarks/TapeWriteBenchmarks2.java)      |    2    | thrpt |    1737.029 |     99.037 | ops/s |
| [tape](./src/main/java/org/infobip/lib/popout/benchmarks/TapeWriteBenchmarks2.java)      |    8    | thrpt |    1730.007 |     80.156 | ops/s |

**HDD** slow VPS machine:

| Benchmark                                                                                | Threads | Mode  | Score      | Error     | Units |
|:-----------------------------------------------------------------------------------------|:-------:|:-----:|-----------:|----------:|:------|
| [batched](./src/main/java/org/infobip/lib/popout/benchmarks/BatchedWriteBenchmarks.java) |    1    | thrpt | 340561.607 | 10061.559 | ops/s |
| [batched](./src/main/java/org/infobip/lib/popout/benchmarks/BatchedWriteBenchmarks.java) |    2    | thrpt | 335203.438 | 17237.922 | ops/s |
| [batched](./src/main/java/org/infobip/lib/popout/benchmarks/BatchedWriteBenchmarks.java) |    8    | thrpt |  82027.788 |  4933.303 | ops/s |
| [synced](./src/main/java/org/infobip/lib/popout/benchmarks/SyncedWriteBenchmarks.java)   |    1    | thrpt |   4055.127 |   242.564 | ops/s |
| [synced](./src/main/java/org/infobip/lib/popout/benchmarks/SyncedWriteBenchmarks.java)   |    2    | thrpt |   3898.890 |   327.030 | ops/s |
| [synced](./src/main/java/org/infobip/lib/popout/benchmarks/SyncedWriteBenchmarks.java)   |    8    | thrpt |   3984.240 |   186.945 | ops/s |
| [tape](./src/main/java/org/infobip/lib/popout/benchmarks/TapeWriteBenchmarks2.java)      |    1    | thrpt |    627.227 |    45.789 | ops/s |
| [tape](./src/main/java/org/infobip/lib/popout/benchmarks/TapeWriteBenchmarks2.java)      |    2    | thrpt |    624.004 |    47.643 | ops/s |
| [tape](./src/main/java/org/infobip/lib/popout/benchmarks/TapeWriteBenchmarks2.java)      |    8    | thrpt |    628.569 |    18.652 | ops/s |

### Iteration over queue from disk

- **JMH version:** 1.21
- **VM version:** JDK 1.8.0_212, Java HotSpot(TM) 64-Bit Server VM, 25.212-b10
- **VM options:** -Xms256m -Xmx1G
- **Warmup:** 1 iterations, single-shot each
- **Measurement:** 3 iterations, single-shot each
- **Timeout:** 10 min per iteration
- **Benchmark mode:** Single shot invocation time

Here we tests the queue's iterators from different implementations. Preliminarily, we writes **one milliom** records of 512 bytes length data to disk, then iterates it via `batched`/`synced` iterators.

> **NOTICE:** `tape` is not present here, because its initialization (writing 1_000_000 records) takes too much time (almost 30 minutes) and `JMH` interupts it...

**SSD** `DigitalOcean` droplet:

| Benchmark                                                                                   | Mode | Score  | Error | Units |
|:--------------------------------------------------------------------------------------------|:----:|-------:|------:|:------|
| [batched](./src/main/java/org/infobip/lib/popout/benchmarks/BatchedIteratorBenchmarks.java) |  ss  |  6.549 | 0.459 |  s/op |
| [synced](./src/main/java/org/infobip/lib/popout/benchmarks/SyncedIteratorBenchmarks.java)   |  ss  | 63.947 | 5.072 |  s/op |
| [tape](./src/main/java/org/infobip/lib/popout/benchmarks/TapeIteratorBenchmarks.java)       |  ss  |    -    |    -   |  s/op |

**HDD** slow VPS machine:

| Benchmark                                                                                   | Mode | Score   | Error  | Units |
|:--------------------------------------------------------------------------------------------|:----:|--------:|-------:|:------|
| [batched](./src/main/java/org/infobip/lib/popout/benchmarks/BatchedIteratorBenchmarks.java) |  ss  |  32.548 |  3.361 |  s/op |
| [synced](./src/main/java/org/infobip/lib/popout/benchmarks/SyncedIteratorBenchmarks.java)   |  ss  | 115.348 |  3.037 |  s/op |
| [tape](./src/main/java/org/infobip/lib/popout/benchmarks/TapeIteratorBenchmarks.java)       |  ss  |    -    |    -   |  s/op |

### Simultaneous read/write operations

- **JMH version:** 1.21
- **VM version:** JDK 1.8.0_212, Java HotSpot(TM) 64-Bit Server VM, 25.212-b10
- **VM options:** -Xms256m -Xmx1G
- **Warmup:** 1 iterations, 1 min each
- **Measurement:** 3 iterations, 1 min each
- **Timeout:** 10 min per iteration
- **Threads:** 4 threads (1 group; 1x "read", 3x "write" in each group), will synchronize iterations
- **Benchmark mode:** Throughput, ops/time

In this test we have simultaneous write and read operations. There 3 threads to write and 1 thread to read in each test case.

**SSD** `DigitalOcean` droplet:

> **NOTICE:** `tape` has a "good" score, because of fast reads (around **30000**)...but it is rash reads of `NULL`s. Take a look at the write score - nearly **1600**, so the queue is almost always empty.

| Benchmark                                                                                    | Description   | Mode  | Score       | Error     | Units |
|:---------------------------------------------------------------------------------------------|:-------------:|:-----:|------------:|----------:|:------|
| [batched](./src/main/java/org/infobip/lib/popout/benchmarks/BatchedReadWriteBenchmarks.java) | write + read  | thrpt |  429808.465 | 21900.206 | ops/s |
| [batched](./src/main/java/org/infobip/lib/popout/benchmarks/BatchedReadWriteBenchmarks.java) |     read      | thrpt |  214922.413 | 10854.872 | ops/s |
| [batched](./src/main/java/org/infobip/lib/popout/benchmarks/BatchedReadWriteBenchmarks.java) |     write     | thrpt |  214886.052 | 11046.534 | ops/s |
| [synced](./src/main/java/org/infobip/lib/popout/benchmarks/SyncedReadWriteBenchmarks.java)   | write + read  | thrpt |   19979.111 |  1813.331 | ops/s |
| [synced](./src/main/java/org/infobip/lib/popout/benchmarks/SyncedReadWriteBenchmarks.java)   |     read      | thrpt |    9979.437 |   906.492 | ops/s |
| [synced](./src/main/java/org/infobip/lib/popout/benchmarks/SyncedReadWriteBenchmarks.java)   |     write     | thrpt |    9992.674 |   906.847 | ops/s |
| [tape](./src/main/java/org/infobip/lib/popout/benchmarks/TapeReadWriteBenchmarks.java)       | write + read  | thrpt |   32328.193 |  1500.906 | ops/s |
| [tape](./src/main/java/org/infobip/lib/popout/benchmarks/TapeReadWriteBenchmarks.java)       |     read      | thrpt |   30704.119 |  1413.401 | ops/s |
| [tape](./src/main/java/org/infobip/lib/popout/benchmarks/TapeReadWriteBenchmarks.java)       |     write     | thrpt |    1624.075 |    97.712 | ops/s |

**HDD** slow VPS machine:

> **NOTICE:** `tape` has a "good" score, because of fast reads (around **6500**)...but it is rash reads of `NULL`s. Take a look at the write score - nearly **600**, so the queue is almost always empty.

| Benchmark                                                                                    | Description   | Mode  | Score       | Error     | Units |
|:---------------------------------------------------------------------------------------------|:-------------:|:-----:|------ -----:|----------:|:------|
| [batched](./src/main/java/org/infobip/lib/popout/benchmarks/BatchedReadWriteBenchmarks.java) | write + read  | thrpt |  125998.278 | 11024.132 | ops/s |
| [batched](./src/main/java/org/infobip/lib/popout/benchmarks/BatchedReadWriteBenchmarks.java) |     read      | thrpt |   63031.627 |  5524.542 | ops/s |
| [batched](./src/main/java/org/infobip/lib/popout/benchmarks/BatchedReadWriteBenchmarks.java) |     write     | thrpt |   62966.651 |  5500.945 | ops/s |
| [synced](./src/main/java/org/infobip/lib/popout/benchmarks/SyncedReadWriteBenchmarks.java)   | write + read  | thrpt |    6597.817 |   381.055 | ops/s |
| [synced](./src/main/java/org/infobip/lib/popout/benchmarks/SyncedReadWriteBenchmarks.java)   |     read      | thrpt |    3305.614 |   155.694 | ops/s |
| [synced](./src/main/java/org/infobip/lib/popout/benchmarks/SyncedReadWriteBenchmarks.java)   |     write     | thrpt |    3292.204 |   250.553 | ops/s |
| [tape](./src/main/java/org/infobip/lib/popout/benchmarks/TapeReadWriteBenchmarks.java)       | write + read  | thrpt |    7104.478 |   785.961 | ops/s |
| [tape](./src/main/java/org/infobip/lib/popout/benchmarks/TapeReadWriteBenchmarks.java)       |     read      | thrpt |    6511.880 |   755.388 | ops/s |
| [tape](./src/main/java/org/infobip/lib/popout/benchmarks/TapeReadWriteBenchmarks.java)       |     write     | thrpt |     592.598 |    33.931 | ops/s |

## How to setup the environment

1. Add Java repository:

```bash
$> sudo add-apt-repository --yes ppa:webupd8team/java
```

2. Update and upgrade the distro:

```bash
$> sudo apt-get update --yes && sudo apt-get upgrade --yes
```

3. Install `Git`, `Java 8` and `Maven`:

```bash
$> sudo apt-get install --yes oracle-java8-installer git maven
```

4. Clone the repo:

```bash
$> git clone https://github.com/infobip/popout.git
```

## How to run the benchmarks

1. Go to the project's root:

```bash
$> cd popout
```

2. Build the project with only needed dependencies:

```bash
$> mvn clean package \
     -DskipTests \
     -Dgpg.skip \
     -Dfindbugs.skip=true \
     -Dpmd.skip=true \
     -Dcheckstyle.skip \
     -Dmaven.test.skip=true \
     -pl benchmarks -am
```

3. Run the tests

```bash
$> nohup java -Xms256m -Xmx1G -jar benchmarks/target/benchmarks-*-capsule.jar > job.logs 2>&1 &
```

### One-liner

```bash
$> sudo add-apt-repository --yes ppa:webupd8team/java && \
   sudo apt-get update --yes && sudo apt-get upgrade --yes && \
   sudo apt-get install --yes oracle-java8-installer git maven && \
   git clone https://github.com/infobip/popout.git && \
   cd popout && \
   mvn clean package \
     -DskipTests \
     -Dgpg.skip \
     -Dfindbugs.skip=true \
     -Dpmd.skip=true \
     -Dcheckstyle.skip \
     -Dmaven.test.skip=true \
     -pl benchmark -am && \
  nohup java -Xms256m -Xmx1G -jar benchmarks/target/benchmarks-*-capsule.jar > job.logs 2>&1 &
```

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
| [Batched](./src/main/java/org/infobip/lib/popout/benchmarks/BatchedWriteBenchmarks.java) |    1    | thrpt |  416493.732 |  48490.629 | ops/s |
| [Batched](./src/main/java/org/infobip/lib/popout/benchmarks/BatchedWriteBenchmarks.java) |    2    | thrpt |  431726.683 |  38066.841 | ops/s |
| [Batched](./src/main/java/org/infobip/lib/popout/benchmarks/BatchedWriteBenchmarks.java) |    8    | thrpt |  201545.938 |   9202.873 | ops/s |
| [Synced](./src/main/java/org/infobip/lib/popout/benchmarks/SyncedWriteBenchmarks.java)   |    1    | thrpt |    9142.644 |    631.290 | ops/s |
| [Synced](./src/main/java/org/infobip/lib/popout/benchmarks/SyncedWriteBenchmarks.java)   |    2    | thrpt |    9130.862 |    110.159 | ops/s |
| [Synced](./src/main/java/org/infobip/lib/popout/benchmarks/SyncedWriteBenchmarks.java)   |    8    | thrpt |    8758.429 |    856.279 | ops/s |

**HDD** slow VPS machine:

| Benchmark                                                                                | Threads | Mode  | Score       | Error      | Units |
|:-----------------------------------------------------------------------------------------|:-------:|:-----:|------------:|-----------:|:------|
| [Batched](./src/main/java/org/infobip/lib/popout/benchmarks/BatchedWriteBenchmarks.java) |    1    | thrpt |  339326.888 |  30363.049 | ops/s |
| [Batched](./src/main/java/org/infobip/lib/popout/benchmarks/BatchedWriteBenchmarks.java) |    2    | thrpt |  334198.205 |  18257.508 | ops/s |
| [Batched](./src/main/java/org/infobip/lib/popout/benchmarks/BatchedWriteBenchmarks.java) |    8    | thrpt |   82094.633 |   5595.660 | ops/s |
| [Synced](./src/main/java/org/infobip/lib/popout/benchmarks/SyncedWriteBenchmarks.java)   |    1    | thrpt |    4032.630 |    361.506 | ops/s |
| [Synced](./src/main/java/org/infobip/lib/popout/benchmarks/SyncedWriteBenchmarks.java)   |    2    | thrpt |    3973.133 |    270.485 | ops/s |
| [Synced](./src/main/java/org/infobip/lib/popout/benchmarks/SyncedWriteBenchmarks.java)   |    8    | thrpt |    3912.688 |    388.949 | ops/s |

### Iteration over queue from disk

- **JMH version:** 1.21
- **VM version:** JDK 1.8.0_212, Java HotSpot(TM) 64-Bit Server VM, 25.212-b10
- **VM options:** -Xms256m -Xmx1G
- **Warmup:** 1 iterations, single-shot each
- **Measurement:** 3 iterations, single-shot each
- **Timeout:** 10 min per iteration
- **Benchmark mode:** Single shot invocation time

Here we tests the queue's iterators from different implementations. Preliminarily, we writes **one milliom** records of 512 bytes length data to disk, then iterates it via `batched`/`synced` iterators.

**SSD** `DigitalOcean` droplet:

| Benchmark                                                                                   | Mode | Score  | Error | Units |
|:--------------------------------------------------------------------------------------------|:----:|-------:|------:|:------|
| [batched](./src/main/java/org/infobip/lib/popout/benchmarks/BatchedIteratorBenchmarks.java) |  ss  |  7.947 | 0.409 |  s/op |
| [synced](./src/main/java/org/infobip/lib/popout/benchmarks/SyncedIteratorBenchmarks.java)   |  ss  | 76.041 | 2.770 |  s/op |

**HDD** slow VPS machine:

| Benchmark                                                                                   | Mode | Score   | Error  | Units |
|:--------------------------------------------------------------------------------------------|:----:|--------:|-------:|:------|
| [batched](./src/main/java/org/infobip/lib/popout/benchmarks/BatchedIteratorBenchmarks.java) |  ss  |  31.665 |  0.707 |  s/op |
| [synced](./src/main/java/org/infobip/lib/popout/benchmarks/SyncedIteratorBenchmarks.java)   |  ss  | 113.947 |  4.446 |  s/op |

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

| Benchmark                                                                                    | Description   | Mode  | Score       | Error     | Units |
|:---------------------------------------------------------------------------------------------|:-------------:|:-----:|------------:|----------:|:------|
| [batched](./src/main/java/org/infobip/lib/popout/benchmarks/BatchedReadWriteBenchmarks.java) | write + read  | thrpt |  356000.071 | 10689.895 | ops/s |
| [batched](./src/main/java/org/infobip/lib/popout/benchmarks/BatchedReadWriteBenchmarks.java) |     read      | thrpt |  177964.106 |  5317.090 | ops/s |
| [batched](./src/main/java/org/infobip/lib/popout/benchmarks/BatchedReadWriteBenchmarks.java) |     write     | thrpt |  178035.966 |  5376.288 | ops/s |
| [synced](./src/main/java/org/infobip/lib/popout/benchmarks/SyncedReadWriteBenchmarks.java)   | write + read  | thrpt |   14696.753 |  1108.438 | ops/s |
| [synced](./src/main/java/org/infobip/lib/popout/benchmarks/SyncedReadWriteBenchmarks.java)   |     read      | thrpt |    7347.880 |   579.236 | ops/s |
| [synced](./src/main/java/org/infobip/lib/popout/benchmarks/SyncedReadWriteBenchmarks.java)   |     write     | thrpt |    7348.873 |   532.571 | ops/s |

**HDD** slow VPS machine:

| Benchmark                                                                                    | Description   | Mode  | Score       | Error     | Units |
|:---------------------------------------------------------------------------------------------|:-------------:|:-----:|------ -----:|----------:|:------|
| [batched](./src/main/java/org/infobip/lib/popout/benchmarks/BatchedReadWriteBenchmarks.java) | write + read  | thrpt |  129047.209 |   7322.019 | ops/s |
| [batched](./src/main/java/org/infobip/lib/popout/benchmarks/BatchedReadWriteBenchmarks.java) |     read      | thrpt |   64560.194 |   3678.515 | ops/s |
| [batched](./src/main/java/org/infobip/lib/popout/benchmarks/BatchedReadWriteBenchmarks.java) |     write     | thrpt |   64487.015 |   3644.530 | ops/s |
| [synced](./src/main/java/org/infobip/lib/popout/benchmarks/SyncedReadWriteBenchmarks.java)   | write + read  | thrpt |    6671.062 |    550.223 | ops/s |
| [synced](./src/main/java/org/infobip/lib/popout/benchmarks/SyncedReadWriteBenchmarks.java)   |     read      | thrpt |    3332.767 |    290.337 | ops/s |
| [synced](./src/main/java/org/infobip/lib/popout/benchmarks/SyncedReadWriteBenchmarks.java)   |     write     | thrpt |    3338.295 |    262.319 | ops/s |

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

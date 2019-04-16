# Overview

## Benchmark host machine

For benchmarking we are using two different machines:

- small, but fast, `DigitalOcean`'s droplet with **SSD** drive;
- VPS with slow **HDD** disk.

## The results

### Write operations

- **JMH version:** 1.21
- **VM version:** JDK 1.8.0_202, Java HotSpot(TM) 64-Bit Server VM, 25.202-b08
- **VM options:** -Xms256m -Xmx1G
- **Warmup:** 1 iterations, 1 min each
- **Measurement:** 3 iterations, 1 min each
- **Timeout:** 10 min per iteration
- **Benchmark mode:** Throughput, ops/time

**SSD** `DigitalOcean` droplet:

| Benchmark                              | Mode  | Score       |      Error | Units |
|:---------------------------------------|:-----:|------------:|-----------:|:------|
| BatchedWriteBenchmarks.write_threads_1 | thrpt |  390608.646 |  65620.533 | ops/s |
| BatchedWriteBenchmarks.write_threads_2 | thrpt |  401681.251 |  15727.514 | ops/s |
| BatchedWriteBenchmarks.write_threads_8 | thrpt |  223740.048 |   4260.939 | ops/s |
| SyncedWriteBenchmarks.write_threads_1  | thrpt |   12463.132 |    676.496 | ops/s |
| SyncedWriteBenchmarks.write_threads_2  | thrpt |   12491.883 |    988.345 | ops/s |
| SyncedWriteBenchmarks.write_threads_8  | thrpt |   12886.055 |    623.271 | ops/s |

**HDD** slow VPS machine:

| Benchmark                              | Mode  | Score       |      Error | Units |
|:---------------------------------------|:-----:|------------:|-----------:|:------|
| BatchedWriteBenchmarks.write_threads_1 | thrpt |  340781.298 |  11799.089 | ops/s |
| BatchedWriteBenchmarks.write_threads_2 | thrpt |  336733.038 |  20886.520 | ops/s |
| BatchedWriteBenchmarks.write_threads_8 | thrpt |   82755.301 |   3561.014 | ops/s |
| SyncedWriteBenchmarks.write_threads_1  | thrpt |    4065.916 |    394.432 | ops/s |
| SyncedWriteBenchmarks.write_threads_2  | thrpt |    3984.433 |    359.567 | ops/s |
| SyncedWriteBenchmarks.write_threads_8  | thrpt |    4009.548 |    219.911 | ops/s |

### Simultaneous read/write operations

- **JMH version:** 1.21
- **VM version:** JDK 1.8.0_202, Java HotSpot(TM) 64-Bit Server VM, 25.202-b08
- **VM options:** -Xms256m -Xmx1G
- **Warmup:** 1 iterations, 1 min each
- **Measurement:** 3 iterations, 1 min each
- **Timeout:** 10 min per iteration
- **Threads:** 4 threads (1 group; 1x "read", 3x "write" in each group), will synchronize iterations
- **Benchmark mode:** Throughput, ops/time

**SSD** `DigitalOcean` droplet:

| Benchmark                                           | Mode  | Score       |      Error | Units |
|:----------------------------------------------------|:-----:|------------:|-----------:|:------|
| BatchedReadWriteBenchmarks.batched_read_write       | thrpt |  433546.427 |   9949.984 | ops/s |
| BatchedReadWriteBenchmarks.batched_read_write:read  | thrpt |  216801.149 |   5071.868 | ops/s |
| BatchedReadWriteBenchmarks.batched_read_write:write | thrpt |  216745.278 |   4879.510 | ops/s |
| SyncedReadWriteBenchmarks.synced_read_write         | thrpt |   20504.507 |   1131.706 | ops/s |
| SyncedReadWriteBenchmarks.synced_read_write:read    | thrpt |   10248.779 |    563.981 | ops/s |
| SyncedReadWriteBenchmarks.synced_read_write:write   | thrpt |   10255.728 |    568.286 | ops/s |

**HDD** slow VPS machine:

| Benchmark                                           | Mode  | Score       |      Error | Units |
|:----------------------------------------------------|:-----:|------------:|-----------:|:------|
| BatchedReadWriteBenchmarks.batched_read_write       | thrpt |  128997.211 |   3545.615 | ops/s |
| BatchedReadWriteBenchmarks.batched_read_write:read  | thrpt |   64480.888 |   1834.657 | ops/s |
| BatchedReadWriteBenchmarks.batched_read_write:write | thrpt |   64516.322 |   1713.174 | ops/s |
| SyncedReadWriteBenchmarks.synced_read_write         | thrpt |    6631.695 |    405.777 | ops/s |
| SyncedReadWriteBenchmarks.synced_read_write:read    | thrpt |    3315.911 |    202.354 | ops/s |
| SyncedReadWriteBenchmarks.synced_read_write:write   | thrpt |    3315.783 |    213.629 | ops/s |

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

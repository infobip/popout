
# Popout

[![build_status](https://travis-ci.org/infobip/popout.svg?branch=master)](https://travis-ci.org/infobip/popout)
[![maven_central](https://maven-badges.herokuapp.com/maven-central/org.infobip.lib/popout/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.infobip.lib/popout)
[![License](http://img.shields.io/:license-apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)
[![JavaDoc](http://www.javadoc.io/badge/org.infobip.lib/popout.svg)](http://www.javadoc.io/doc/org.infobip.lib/popout)

`Popout` is a file-based queue for Java.

Don't forget to take a look at our [benchmarks](./benchmarks/README.md).

## Contents

- [Requirements](#requirements)
- [Usage](#usage)
  - [Add dependency](#add-dependency)
  - [Create queue](#create-queue)
  - [Basic operations](#basic-operations)
  - [Custom serialization and deserialization](#custom-serialization-and-deserialization)
- [Development](#development)
  - [Prerequisites](#prerequisites)
  - [Building](#building)
  - [Running the tests](#running-the-tests)
- [Built With](#built-with)
- [Changelog](#changelog)
- [Contributing](#contributing)
- [Versioning](#versioning)
- [Authors](#authors)
- [License](#license)

## Requirements

- [Java](http://www.oracle.com/technetwork/java/javase) (minimal required version is 8);
- [Maven](https://maven.apache.org)

## Usage

### Add dependency

Include the dependency to your project's pom.xml file:

```xml
<dependencies>
    ...
    <dependency>
        <groupId>org.infobip.lib</groupId>
        <artifactId>popout</artifactId>
        <version>2.0.3</version>
    </dependency>
    ...
</dependencies>
```

or Gradle:

```groovy
compile 'org.infobip.lib:popout:2.0.3'
```

### Create a queue

Let's create a minimal config `FileQueue` instance:

```java
FileQueue<String> queue = FileQueue.<String>synced().build();
```

The code above creates **synced** `FileQueue` implementation with default config. The queue data writes on the disk in a small files, named `WAL`-files. When the amount of that files is sufficiently (specified in the config, see below) that files merges to a big `compressed` file, the next portions of `WAL`-files to the next `compressed`-file and etc.

The differences between the `FileQueue` and the Java-default `java.util.Queue` interfaces are the following:

- `FileQueue`.`longSize` - returns the number of elements in this queue with wide range, than `int`;
- `FileQueue`.`diskSize` - tells the amount of bytes, which the `queue` takes on the disk;
- `FileQueue`.`flush` - flushes all this queue's data to the disk;
- `FileQueue`.`compress` - manually compress all WAL-files into a compressed file;
- `FileQueue`.`close` - flushes and closes the files descriptors of the queue.

There are two main `FileQueue` implementations:

- **synced** - every `add` operation is flushes on disk immediately and every `poll` reads the items from the disk directly. There is no buffers or something in-memory. It suits for cases, when you don't want to lose your data at all and you don't care about performance. It is the most reliable kind of the `FileQueue`;

- **batched** - a concept of `tail` and `head` buffers is present here. You can specify a `batchSize` option, which tells to the queue builder how many elements could be store in memory, before writing to the disk. Writes and reads to/from the disk operations are batched and it boosts the queue's performance, but you always should remember that in case of unexpected crash you could lose your *head* or *tail* data. This kind of queue suits well when your need more performant queue and you don't afraid to lose some amount of data, or you are ready to control it your self by periodically invoking the `flush` method.

> **NOTICE:** you also could instantiate WAL `maxCount` option and `batchSize` to `Integer.MAX_VALUE` and use `flush` and `compress` by yourself in fully manual manner.

More advanced `FileQueue` usage:

```java
Queue<Integer> queue = FileQueue.<Integer>batched()
        // the name of the queue, used in file patterns
        .name("popa")
        // the default folder for all queue's files
        .folder("/folder/where/store/queue/files")
        // sets custom serializer
        .serializer(Serializer.INTEGER)
        // sets custom deserializer
        .deserializer(Deserializer.INTEGER)
        // set up the queue's limits settings
        .limit(QueueLimit.queueLength()
                .length(1_000_000)
                .handler(myQueueLimitExceededHandler))
        // restores from disk or not, during startup. If 'false' - the previous files will be removed
        .restoreFromDisk(false)
        // WAL files configuration
        .wal(WalFilesConfig.builder()
            // the place where WAL files stores. Default is a queue's folder above
            .folder("some/wal/files/folder")
            // the maximum allowed amount of WAL files before compression
            .maxCount(1000)
            .build())
        // compressed files config
        .compressed(CompressedFilesConfig.builder()
            // the place where compressed files stores. Default is a queue's folder above
            .folder("some/compressed/files/folder")
            // the maximum allowed compressed file's size
            .maxSizeBytes(SizeUnit.MEGABYTES.toBytes(256))
            .build())
        // the amount of elements in one WAL file. only batched queue option
        .batchSize(10_000)
        .build();
```

### Basic operations

Add some data to the queue to the end of the queue. `FileQueue` accepts a generic type of arbitrary length:

```java
queue.add("popa");
```

Read data at the head of the queue (without removing it):

```java
String record = queue.peek();
```

In short, `FileQueue` implements `Queue`, `Collection` and `Iterable` interfaces and you are able to use all theirs methods:

```java
// Queue's size
int queueSize = queue.size();

// Add many items
queue.addAll(asList("one", "two", "three"));

// Retrieves and removes the head of this queue,
String head = queue.poll();

// Remove all elements.
queue.clear();

// Use queue's iterator
Iterator<String> iterator = queue.iterator();
```

### Custom serialization and deserialization

By default, queue uses standard [Java's serialization/deserialization mechanism](https://docs.oracle.com/javase/8/docs/technotes/guides/serialization/index.html), but you could override it by implementing [Serializer](https://github.com/infobip/popout/blob/master/popout/src/main/java/org/infobip/lib/popout/Serializer.java) and [Deserializer](https://github.com/infobip/popout/blob/master/popout/src/main/java/org/infobip/lib/popout/Deserializer.java):

```java
Queue<String> queue = FileQueue.<String>synced()
        .serializer(<your_serializaer_impl>)
        .deserializer(<your_deserializaer_impl>)
        .build();
```

## Development

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

For building the project you need only a [Java compiler](http://www.oracle.com/technetwork/java/javase/downloads/index.html).

> **IMPORTANT:** Popout requires Java version starting from **8**

And, of course, you need to clone Popout from GitHub:

```bash
$> git clone https://github.com/infobip/popout
$> cd popout
```

### Building

For building routine automation, I am using [maven](https://maven.apache.org).

To build the Popout project, do the following:

```bash
$> mvn clean package
...
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 35.491 s
[INFO] Finished at: 2019-01-18T23:25:12+03:00
[INFO] Final Memory: 50M/548M
[INFO] ------------------------------------------------------------------------
```

### Running the tests

To run the project's test, do the following:

```bash
$> mvn clean test
...
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
...
[INFO]
[INFO] Results:
[INFO]
[INFO] Tests run: 32, Failures: 0, Errors: 0, Skipped: 0
[INFO]
...
```

Also, if you do `package` or `install` goals, the tests launch automatically.

## Built With

- [Java](http://www.oracle.com/technetwork/java/javase) - is a systems and applications programming language
- [Lombok](https://projectlombok.org) - is a java library that spicing up your java
- [Junit](http://junit.org/junit4/) - is a simple framework to write repeatable tests
- [AssertJ](http://joel-costigliola.github.io/assertj/) - AssertJ provides a rich set of assertions, truly helpful error messages, improves test code readability
- [Maven](https://maven.apache.org) - is a software project management and comprehension tool

## Changelog

To see what has changed in recent versions of Popout, see the [changelog](./CHANGELOG.md) file.

## Contributing

Please read [contributing](./CONTRIBUTING.md) file for details on my code of conduct, and the process for submitting pull requests to me.

## Versioning

We use [SemVer](http://semver.org/) for versioning. For the versions available, see the [tags on this repository](https://github.com/infobip/popout/tags).

## Authors

- **[Artem Labazin](https://github.com/xxlabaza)** - creator and the main developer

## License

This project is licensed under the Apache License 2.0 License - see the [license](./LICENSE) file for details

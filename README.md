
# Popout

[![build_status](https://travis-ci.org/infobip/popout.svg?branch=master)](https://travis-ci.org/infobip/popout)
[![maven_central](https://maven-badges.herokuapp.com/maven-central/org.infobip.lib/popout/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.infobip.lib/popout)

`Popout` is a file-based queue for Java.

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

* [Java](http://www.oracle.com/technetwork/java/javase) (minimal required version is 8);
* [Maven](https://maven.apache.org)

## Usage

### Add dependency

Include the dependency to your project's pom.xml file:

```xml
<dependencies>
    ...
    <dependency>
        <groupId>org.infobip.lib</groupId>
        <artifactId>popout</artifactId>
        <version>1.1.0</version>
    </dependency>
    ...
</dependencies>
```

or Gradle:

```groovy
compile 'org.infobip.lib:poput:1.1.0'
```

### Create queue

Create `FileQueue` instance:

```java
Path folder = // ...

FileQueue<String> queue = FileQueue.<String>in(folder).build();
```

The code above creates default `FileQueue` implementation with write/read operations via FileChannel, maximum file size up to 100mb, `batch-#.queue` naming pattern for queue files (where your records are) and `queue.metadata` metadata file (`head`, `tail` positions and `elements` counter).

More advanced `FileQueue` usage:

```java
Queue<Integer> queue = FileQueue.<Integer>in("/folder/where/store/queue/files")
        // path to metadata file, where additional info stores
        .metadataFile("/another/folder/because/i/can/why_not.metadata")
        // file pattern for queue files, where records stores
        .filePattern("nice-queue-file-#.queuefile")
        // maximum file size for queue file in bytes
        .maxFileSizeBytes(100)
        // enables using mmap files and setting buffer size in bytes (default is 8192)
        .fileMmapEnable(50)
        // sets custom serializer
        .serializer(new DefaultSerializer())
        // sets custom deserializer
        .deserializer(new DefaultDeserializer())
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

By default, queue uses standard [Java's serialization/deserialization mechanism](https://docs.oracle.com/javase/8/docs/technotes/guides/serialization/index.html), but you could override it by implementing [Serializer](https://github.com/infobip/popout/blob/master/src/main/java/org/infobip/lib/popout/writer/Serializer.java) and [Deserializer](https://github.com/infobip/popout/blob/master/src/main/java/org/infobip/lib/popout/reader/Deserializer.java):

```java
Queue<String> queue = FileQueue.<String>in(folder)
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
[INFO] Finished at: 2018-01-18T23:25:12+03:00
[INFO] Final Memory: 50M/548M
[INFO] ------------------------------------------------------------------------
```

> **IMPORTANT:** If you use Java 9, add profile `jdk9` to your goals, like this:
>
> ```bash
> $> mvn clean package -Pjdk9
> ```

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
[INFO] Tests run: 54, Failures: 0, Errors: 0, Skipped: 0
[INFO]
...
```

Also, if you do `package` or `install` goals, the tests launch automatically.

## Built With

* [Java](http://www.oracle.com/technetwork/java/javase) - is a systems and applications programming language

* [Lombok](https://projectlombok.org) - is a java library that spicing up your java

* [Junit](http://junit.org/junit4/) - is a simple framework to write repeatable tests

* [AssertJ](http://joel-costigliola.github.io/assertj/) - AssertJ provides a rich set of assertions, truly helpful error messages, improves test code readability

* [Maven](https://maven.apache.org) - is a software project management and comprehension tool

## Changelog

To see what has changed in recent versions of Popout, see the [changelog](./CHANGELOG.md) file.

## Contributing

Please read [contributing](./CONTRIBUTING.md) file for details on my code of conduct, and the process for submitting pull requests to me.

## Versioning

We use [SemVer](http://semver.org/) for versioning. For the versions available, see the [tags on this repository](https://github.com/infobip/popout/tags).

## Authors

* **[Artem Labazin](https://github.com/xxlabaza)** - creator and the main developer

## License

This project is licensed under the Apache License 2.0 License - see the [license](./LICENSE) file for details


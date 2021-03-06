
# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

[Tags on this repository](https://github.com/infobip/popout/tags)

## [Unreleased]

- Add batch write/read (using FileChannel) with customizable batch size;
- Add concurrent access to queue (methods with `synchronized` keyword or based on locks).

## [2.1.1](https://github.com/infobip/popout/releases/tag/2.1.1) - 2019-09-25

### Fixed

- `SyncedFileQueue` don't fall in infinity loop during restoring from backup.

## [2.1.0](https://github.com/infobip/popout/releases/tag/2.1.0) - 2019-04-28

### Added

- `AutoCloseable` interface for backend services;
- `FileQueueBuilder`.`corruptionHandler` - a handler for corrupted data from disk;
- `CorruptedDataException` and `ReadingFromDiskException` for different IO reading exceptions.

### Changed

- `BatchedFileQueue` now **clear** its tail on flush, instead of creating new.

## [2.0.4](https://github.com/infobip/popout/releases/tag/2.0.4) - 2019-04-25

### Changed

- `FilesManager`.`getFilesFromFileSystem` returns *fast* empty `Queue` if tracked folder is empty.

## [2.0.3](https://github.com/infobip/popout/releases/tag/2.0.3) - 2019-04-20

### Changed

- Changed parent's POM dependency relation.

## [2.0.2](https://github.com/infobip/popout/releases/tag/2.0.2) - 2019-04-19

### Changed

- Refactored dependencies management.

## [2.0.1](https://github.com/infobip/popout/releases/tag/2.0.1) - 2019-04-17

### Added

- `Batched` and `synced` file queue implementations.

### Removed

- Working with mmap-files.

## [1.1.0](https://github.com/infobip/popout/releases/tag/1.1.0) - 2018-01-23

Minor update with set of small fixes and Java 9 support.

### Added

- Java 9 support;
- JDK9 build profile.

## [1.0.0](https://github.com/infobip/popout/releases/tag/1.0.0) - 2018-01-18

Initial release.

### Added

- Created nice FileQueue builder
- FileChannel implementation.
- Mmap implementation.
- Basic tests.

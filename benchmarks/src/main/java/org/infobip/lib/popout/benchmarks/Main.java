package org.infobip.lib.popout.benchmarks;

import static io.appulse.utils.SizeUnit.MEGABYTES;
import static java.util.Comparator.reverseOrder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ThreadLocalRandom;

import org.infobip.lib.popout.CompressedFilesConfig;
import org.infobip.lib.popout.Deserializer;
import org.infobip.lib.popout.FileQueue;
import org.infobip.lib.popout.QueueLimit;
import org.infobip.lib.popout.Serializer;
import org.infobip.lib.popout.WalFilesConfig;
import org.infobip.lib.popout.batched.BatchedFileQueueBuilder;

import lombok.SneakyThrows;
import lombok.val;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

  private static final Path TEST_FILES = Paths.get("./tests_files");

  public static void main (String[] args) throws Exception {
    deleteFolder(TEST_FILES);
    Files.createDirectories(TEST_FILES);

    byte[] payload = new byte[512];
    ThreadLocalRandom.current().nextBytes(payload);
    val builder = queueBuilder();

    try (val queue = builder.build()) {
      log.info("Creating temporary files for READ benchmark tests");
      for (int i = 0; i < 25_000_000; i++) {
        queue.add(payload);
      }
      log.info("Creating files end");

      log.info("Start polling");
      do {
        try {
          val item = queue.poll();
          if (item == null) {
            break;
          }
        } catch (Throwable ex) {
          log.error("ERROR", ex);
          System.exit(1);
        }
      } while (true);
      log.info("End polling");
    }
  }

  private static BatchedFileQueueBuilder<byte[]> queueBuilder () {
    return FileQueue.<byte[]>batched()
        .name("my-test")
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
  }

  @SneakyThrows
  private static void deleteFolder (Path path) {
    if (Files.notExists(path)) {
      return;
    }

    Files.walk(path)
        .sorted(reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete);
  }
}

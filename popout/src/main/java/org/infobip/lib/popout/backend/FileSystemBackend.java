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

package org.infobip.lib.popout.backend;

import static lombok.AccessLevel.PRIVATE;

import java.util.Iterator;

import org.infobip.lib.popout.CompressedFilesConfig;
import org.infobip.lib.popout.WalFilesConfig;

import io.appulse.utils.Bytes;
import lombok.Builder;
import lombok.NonNull;
import lombok.experimental.FieldDefaults;
import lombok.val;

/**
 * The umbrella class for WAL and compressed files, for effective working with them.
 */
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class FileSystemBackend implements Iterable<WalContent> {

  WalFiles walFiles;

  CompressedFiles compressedFiles;

  /**
   * Constructor.
   *
   * @param queueName the queue name for file names pattern
   *
   * @param walConfig the configuration for WAL files
   *
   * @param compressedConfig the configuration for compressed files
   *
   * @param restoreFromDisk the flag which tells should it restores from disk or not
   */
  @Builder
  public FileSystemBackend (@NonNull String queueName,
                            @NonNull WalFilesConfig walConfig,
                            @NonNull CompressedFilesConfig compressedConfig,
                            @NonNull Boolean restoreFromDisk
  ) {
    walFiles = WalFiles.builder()
        .queueName(queueName)
        .restoreFromDisk(restoreFromDisk)
        .config(walConfig)
        .build();

    compressedFiles = CompressedFiles.builder()
        .queueName(queueName)
        .restoreFromDisk(restoreFromDisk)
        .config(compressedConfig)
        .build();
  }

  /**
   * Writes the {@code buffer} content to the next WAL file.
   * If the wal files limit exceeded - merge them into a new compress file and remove.
   *
   * @param buffer byte array source to write
   */
  public void write (@NonNull Bytes buffer) {
    walFiles.write(buffer);

    while (walFiles.isLimitExceeded()) {
      val files = walFiles.getFiles();
      val result = compressedFiles.compress(files);
      walFiles.remove(result.getCompressed());
    }
  }

  /**
   * Retrieves and removes the head of the next WAL content into the {@code buffer}.
   *
   * @param buffer the destination bytes buffer, where content writes
   *
   * @return number of written bytes into {@code buffer}
   */
  public int pollTo (@NonNull Bytes buffer) {
    val readed = compressedFiles.pollContentPart(buffer);
    return readed > 0
           ? readed
           : walFiles.pollTo(buffer);
  }

  /**
   * Retrieves, but does not remove the next WAL content into the {@code buffer}.
   *
   * @param buffer the destination bytes buffer
   *
   * @return number of written bytes into {@code buffer}
   */
  public int peakTo (@NonNull Bytes buffer) {
    val readed = compressedFiles.peekContentPart(buffer);
    return readed > 0
           ? readed
           : walFiles.peakTo(buffer);
  }

  /**
   * Returns the size, which is occupied by files related to this backend (WAL and compressed fiels).
   *
   * @return number of bytes, the backend takes on the disk
   */
  public long diskSize () {
    return walFiles.diskSize() + compressedFiles.diskSize();
  }

  @Override
  public Iterator<WalContent> iterator () {
    return new FileSystemBackendIterator();
  }

  @FieldDefaults(level = PRIVATE)
  private class FileSystemBackendIterator implements Iterator<WalContent> {

    final Iterator<WalContent> compressed = compressedFiles.iterator();

    final Iterator<WalContent> wals = walFiles.iterator();

    Iterator<WalContent> current = compressed;

    @Override
    public boolean hasNext () {
      if (current.hasNext()) {
        return true;
      } else if (wals.hasNext()) {
        current = wals;
        return true;
      }
      return false;
    }

    @Override
    public WalContent next () {
      return current.next();
    }

    @Override
    public void remove () {
      current.remove();
    }
  }
}

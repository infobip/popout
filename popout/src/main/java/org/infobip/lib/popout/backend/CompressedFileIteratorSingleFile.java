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

import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;

import static lombok.AccessLevel.PRIVATE;

import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.NoSuchElementException;

import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.val;

@FieldDefaults(level = PRIVATE)
class CompressedFileIteratorSingleFile implements Iterator<WalContent>, AutoCloseable {

  Path file;

  FileChannel channel;

  RecordHeader header;

  WalContent lastReturned;

  WalContent next;

  @SneakyThrows
  void init (Path path) {
    if (channel != null) {
      close();
    }
    file = path;
    channel = FileChannel.open(path, READ, WRITE);
    if (header == null) {
      header = new RecordHeader();
    }
  }

  @Override
  @SneakyThrows
  public boolean hasNext () {
    if (channel == null) {
      return false;
    }
    if (next != null) {
      return true;
    }
    do {
      header.readFrom(channel);
      if (header.isEnd()) {
        return false;
      } else if (header.isRecord()) {
        break;
      } else if (header.isJump()) {
        val newPosition = header.getValue();
        channel.position(newPosition);
      }
    } while (true);

    next = WalContent.builder()
        .file(file)
        .offset(channel.position())
        .length(header.getLength())
        .build();

    val nextHeaderPosition = channel.position() + header.getLength();
    channel.position(nextHeaderPosition);
    return true;
  }

  @Override
  public WalContent next () {
    if (next != null || hasNext()) {
      lastReturned = next;
      next = null;
      return lastReturned;
    }
    throw new NoSuchElementException();
  }

  @Override
  @SneakyThrows
  public void remove () {
    if (lastReturned == null) {
      throw new IllegalStateException();
    }
    val oldHeaderStartPosition = lastReturned.getOffset() - RecordHeader.BYTES;
    header.writeJump(channel, oldHeaderStartPosition, channel.position());
    lastReturned = null;
  }

  @Override
  public void close () throws Exception {
    if (channel != null && channel.isOpen()) {
      channel.close();
    }
  }
}

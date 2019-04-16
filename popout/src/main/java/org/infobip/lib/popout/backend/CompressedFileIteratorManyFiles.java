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

import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import lombok.experimental.FieldDefaults;
import lombok.val;

@FieldDefaults(level = PRIVATE)
class CompressedFileIteratorManyFiles implements Iterator<WalContent>, AutoCloseable {

  final Iterator<Path> pathsIterator;

  final CompressedFileIteratorSingleFile walContentsIterator;

  WalContent nextWalContent;

  CompressedFileIteratorManyFiles (Collection<Path> paths) {
    pathsIterator = paths.iterator();
    walContentsIterator = new CompressedFileIteratorSingleFile();
  }

  @Override
  public boolean hasNext () {
    if (nextWalContent != null) {
      return true;
    }
    while (!walContentsIterator.hasNext()) {
      if (!pathsIterator.hasNext()) {
        return false;
      }
      val path = pathsIterator.next();
      walContentsIterator.init(path);
    }
    nextWalContent = walContentsIterator.next();
    return true;
  }

  @Override
  public WalContent next () {
    if (nextWalContent != null || hasNext()) {
      val result = nextWalContent;
      nextWalContent = null;
      return result;
    }
    throw new NoSuchElementException();
  }

  @Override
  public void remove () {
    walContentsIterator.remove();
  }

  @Override
  public void close () throws Exception {
    walContentsIterator.close();
  }
}

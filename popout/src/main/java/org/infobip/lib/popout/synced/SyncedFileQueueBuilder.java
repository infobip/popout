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

package org.infobip.lib.popout.synced;

import static lombok.AccessLevel.PRIVATE;

import org.infobip.lib.popout.FileQueue;

import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * The specific builder object for a synced queue instance.
 *
 * @param <T> the type of elements in this queue
 *
 * @author Artem Labazin
 * @since 2.0.1
 */
@Getter
@FieldDefaults(level = PRIVATE)
public class SyncedFileQueueBuilder<T> extends FileQueue.Builder<SyncedFileQueueBuilder<T>, T> {

  @Override
  protected FileQueue<T> createQueue () {
    return new SyncedFileQueue<>(this);
  }
}

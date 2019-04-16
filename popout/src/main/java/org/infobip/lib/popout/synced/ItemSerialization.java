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

import org.infobip.lib.popout.Deserializer;
import org.infobip.lib.popout.Serializer;

import io.appulse.utils.Bytes;
import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.val;

@Builder
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
class ItemSerialization<T> {

  @NonNull
  Serializer<T> serializer;

  @NonNull
  Deserializer<T> deserializer;

  void serialize (T item, Bytes buffer) {
    buffer.reset();
    buffer.write4B(0);
    serializer.serialize(item, buffer);
    buffer.set4B(0, buffer.writerIndex() - Integer.BYTES);
  }

  T deserialize (Bytes buffer) {
    val length = buffer.readInt();
    return deserializer.deserialize(length, buffer);
  }
}

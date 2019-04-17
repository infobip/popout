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

package org.infobip.lib.popout;

import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Wither;

/**
 * The compression files configuration object.
 *
 * @author Artem Labazin
 * @since 2.0.1
 */
@Value
@Wither
@Builder
public class CompressedFilesConfig {

  Path folder;

  Long maxSizeBytes;

  /**
   * The configuration builder.
   */
  public static class CompressedFilesConfigBuilder {

    /**
     * Sets the folder's name where compressed filess will be placed.
     *
     * @param value the new value
     *
     * @return the builder object for chain calls
     */
    public CompressedFilesConfigBuilder folder (@NonNull String value) {
      return folder(Paths.get(value));
    }

    /**
     * Sets the folder's path where compressed filess will be placed.
     *
     * @param value the new value
     *
     * @return the builder object for chain calls
     */
    public CompressedFilesConfigBuilder folder (@NonNull Path value) {
      folder = value;
      return this;
    }

    /**
     * Sets the maximum compressed file size in bytes.
     *
     * @param value the new value
     *
     * @return the builder object for chain calls
     */
    public CompressedFilesConfigBuilder maxSizeBytes (long value) {
      maxSizeBytes = value;
      return this;
    }
  }
}

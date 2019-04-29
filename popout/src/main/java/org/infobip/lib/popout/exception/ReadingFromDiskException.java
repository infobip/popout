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

package org.infobip.lib.popout.exception;

import static java.util.Locale.ENGLISH;
import static lombok.AccessLevel.PRIVATE;

import java.nio.file.Path;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

/**
 * Generic IO runtime-exception.
 *
 * @since 2.1.0
 * @author Artem Labazin
 */
@Getter
@ToString
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class ReadingFromDiskException extends RuntimeException {

  private static final long serialVersionUID = 1897411999655926132L;

  private static String getDefaultMessage (Path file) {
    return String.format(ENGLISH, "Error during reading file's '%s' content", file.toAbsolutePath().toString());
  }

  Path file;

  /**
   * Constructs a new reading disk exception.
   *
   * @param file reading file's path
   */
  public ReadingFromDiskException (Path file) {
    this(file, getDefaultMessage(file));
  }

  /**
   * Constructs a new reading disk exception.
   *
   * @param file reading file's path
   *
   * @param message the detail message
   */
  public ReadingFromDiskException (Path file, String message) {
    super(message);
    this.file = file;
  }

  /**
   * Constructs a new reading disk exception.
   *
   * @param file reading file's path
   *
   * @param throwable the cause of this exception
   */
  public ReadingFromDiskException (Path file, Throwable throwable) {
    super(getDefaultMessage(file), throwable);
    this.file = file;
  }

  /**
   * Constructs a new reading disk exception.
   *
   * @param file reading file's path
   *
   * @param message the detail message
   *
   * @param throwable the cause of this exception
   */
  public ReadingFromDiskException (Path file, String message, Throwable throwable) {
    super(message, throwable);
    this.file = file;
  }
}

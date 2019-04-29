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
 * Exception for corrupted files or its parts.
 *
 * @since 2.1.0
 * @author Artem Labazin
 */
@Getter
@ToString
@EqualsAndHashCode(callSuper = true)
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class CorruptedDataException extends ReadingFromDiskException {

  private static final long serialVersionUID = -897411999655926422L;

  private static String getDefaultMessage (Path file, long offset) {
    return String.format(ENGLISH,
        "Corrupted data in file '%s' at position %d",
        file.toAbsolutePath().toString(), offset
    );
  }

  long offset;

  /**
   * Constructs a new corrupted data exception.
   *
   * @param file reading file's path
   *
   * @param offset reading file's offset
   */
  public CorruptedDataException (Path file, long offset) {
    this(file, offset, getDefaultMessage(file, offset));
  }

  /**
   * Constructs a new corrupted data exception.
   *
   * @param file reading file's path
   *
   * @param offset reading file's offset
   *
   * @param message the detail message
   */
  public CorruptedDataException (Path file, long offset, String message) {
    super(file, message);
    this.offset = offset;
  }

  /**
   * Constructs a new corrupted data exception.
   *
   * @param file reading file's path
   *
   * @param offset reading file's offset
   *
   * @param throwable the cause of this exception
   */
  public CorruptedDataException (Path file, long offset, Throwable throwable) {
    this(file, offset, getDefaultMessage(file, offset), throwable);
  }

  /**
   * Constructs a new corrupted data exception.
   *
   * @param file reading file's path
   *
   * @param offset reading file's offset
   *
   * @param message the detail message
   *
   * @param throwable the cause of this exception
   */
  public CorruptedDataException (Path file, long offset, String message, Throwable throwable) {
    super(file, message, throwable);
    this.offset = offset;
  }
}

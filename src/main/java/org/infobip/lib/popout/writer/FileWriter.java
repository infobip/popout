/*
 * Copyright 2018 Infobip Ltd.
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

package org.infobip.lib.popout.writer;

import java.io.Closeable;
import java.nio.file.Path;

/**
 * File's records writer.
 *
 * @since 1.0.0
 * @author Artem Labazin
 */
public interface FileWriter extends Closeable {

    /**
     * Returns current file size.
     *
     * @return file's size
     */
    long currentFileSize ();

    /**
     * Returns current file's position (offset).
     *
     * @return file's offset
     */
    long position ();

    /**
     * Sets reader's offset.
     *
     * @param newPosition new file's offset
     */
    void position (long newPosition);

    /**
     * Returns reader's file path.
     *
     * @return file's path
     */
    Path getPath ();

    /**
     * Writes record's bytes to file.
     *
     * @param bytes record's bytes
     */
    void write (byte[] bytes);

    /**
     * Trims file's size.
     */
    void trim ();
}

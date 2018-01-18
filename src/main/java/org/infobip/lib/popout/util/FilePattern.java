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

package org.infobip.lib.popout.util;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.val;

/**
 * A queue file name pattern utility class.
 *
 * @since 1.0.0
 * @author Artem Labazin
 */
@Value
@AllArgsConstructor
public final class FilePattern {

    /**
     * Parses string representation of {@link FilePattern} and returns its instance.
     *
     * @param string string to parse
     *
     * @return {@link FilePattern} instance from string
     *
     * @throws IllegalArgumentException in case of absent '#' index character
     */
    public static FilePattern from (@NonNull String string) {
        val index = string.indexOf('#');
        if (index == -1) {
            val message = "Invalid file pattern '" + string + "'. It doesn't have index char '#'";
            throw new IllegalArgumentException(message);
        }
        return new FilePattern(
                string.substring(0, index),
                string.substring(index + 1)
        );
    }

    @NonNull
    String prefix;

    @NonNull
    String postfix;

    /**
     * Generates file name with specified index.
     *
     * @param index file name index.
     *
     * @return file name matched by this patter with specified index.
     */
    public String getFileNameWith (int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Index must be greater or equal zero, current is " + index);
        }
        return new StringBuilder()
                .append(prefix).append(index).append(postfix)
                .toString();
    }

    /**
     * Checks, if file names matches by this pattern;
     *
     * @param str file name to check.
     *
     * @return {@code true} - if this pattern matches, {@code false} otherwise.
     */
    public boolean matches (String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        return str.length() > (prefix.length() + postfix.length()) &&
               str.startsWith(prefix) &&
               str.endsWith(postfix);
    }

    /**
     * Extracts index from file name.
     *
     * @param fileName file name.
     *
     * @return extracted index.
     */
    public int extractIndex (String fileName) {
        if (!matches(fileName)) {
            throw new IllegalArgumentException("File name '" + fileName + "' doesn't match the pattern");
        }
        val index = fileName.substring(prefix.length(), fileName.length() - postfix.length());
        return Integer.parseInt(index);
    }
}

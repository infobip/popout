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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;

/**
 *
 * @author Artem Labazin
 */
public class FilePatternTest {

    @Test
    public void from () {
        FilePattern pattern1 = FilePattern.from("batch-#.queue");
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(pattern1).isNotNull();

            softly.assertThat(pattern1.getPrefix())
                    .as("Pattern prefix")
                    .isEqualTo("batch-");

            softly.assertThat(pattern1.getPostfix())
                    .as("Pattern postfix")
                    .isEqualTo(".queue");
        });

        FilePattern pattern2 = FilePattern.from("#.queue");
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(pattern2).isNotNull();

            softly.assertThat(pattern2.getPrefix())
                    .as("Pattern prefix")
                    .isEqualTo("");

            softly.assertThat(pattern2.getPostfix())
                    .as("Pattern postfix")
                    .isEqualTo(".queue");
        });

        FilePattern pattern3 = FilePattern.from("batch-#");
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(pattern3).isNotNull();

            softly.assertThat(pattern3.getPrefix())
                    .as("Pattern prefix")
                    .isEqualTo("batch-");

            softly.assertThat(pattern3.getPostfix())
                    .as("Pattern postfix")
                    .isEqualTo("");
        });

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> FilePattern.from("batch.queue"))
                .withMessage("Invalid file pattern 'batch.queue'. It doesn't have index char '#'");
    }

    @Test
    public void getFileNameWith () {
        FilePattern pattern = new FilePattern("batch-", ".queue");
        assertThat(pattern.getFileNameWith(0))
                .isEqualTo("batch-0.queue");

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> pattern.getFileNameWith(-1))
                .withMessage("Index must be greater or equal zero, current is -1");
    }

    @Test
    public void matches () {
        FilePattern pattern = new FilePattern("batch-", ".queue");
        assertThat(pattern.matches("batch-0.queue")).isTrue();
        assertThat(pattern.matches("batch-999.queue")).isTrue();
        assertThat(pattern.matches("popa")).isFalse();
    }

    @Test
    public void extractIndex () {
        FilePattern pattern = new FilePattern("batch-", ".queue");
        assertThat(pattern.extractIndex("batch-0.queue")).isEqualTo(0);
        assertThat(pattern.extractIndex("batch-999.queue")).isEqualTo(999);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> pattern.extractIndex("popa"))
                .withMessage("File name 'popa' doesn't match the pattern");
    }
}

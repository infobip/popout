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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.junit.Test;

/**
 *
 * @author Artem Labazin
 */
public class ReflectionUtilsTest {

    @Test
    public void getFieldValueFrom () {
        ChildClass object = new ChildClass("Artem", 27);

        assertThat(ReflectionUtils.getFieldValueFrom(object, "name"))
                .isPresent()
                .hasValue("Artem");

        assertThat(ReflectionUtils.getFieldValueFrom(object, "age"))
                .isPresent()
                .hasValue(27);

        assertThat(ReflectionUtils.getFieldValueFrom(object, "popa"))
                .isNotPresent();
    }

    @Test
    public void invokeMethodOf () {
        ChildClass object = new ChildClass("Artem", 27);

        assertThat(ReflectionUtils.invokeMethodOf(object, "getName"))
                .isNotNull()
                .isEqualTo("Artem");

        assertThat(ReflectionUtils.invokeMethodOf(object, "getAge"))
                .isNotNull()
                .isEqualTo(27);

        assertThat(object.isFlag()).isFalse();
        assertThat(ReflectionUtils.invokeMethodOf(object, "setFlag", true))
                .isNull();
        assertThat(object.isFlag()).isTrue();

        assertThat(ReflectionUtils.invokeMethodOf(object, "popa"))
                .isNull();
    }

    @Getter
    @RequiredArgsConstructor
    class ParentClass {

        private final int age;
    }

    @Getter
    class ChildClass extends ParentClass {

        private final String name;

        @Setter
        private boolean flag;

        ChildClass (String name, int age) {
            super(age);
            this.name = name;
        }
    }
}

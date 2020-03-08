/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.typeconversion;

/**
 * 符号转换的结果
 * @param <T>
 */
public interface NotationConvertResult<T> {
    boolean hasResult();

    /**
     * Invoked when a {@link NotationConverter} is able to convert a notation to a result.
     * 当 NotationConverter 能够转换符号时，设置结果
     */
    void converted(T result);
}

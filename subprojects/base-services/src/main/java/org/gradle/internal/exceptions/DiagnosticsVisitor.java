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

package org.gradle.internal.exceptions;

/**
 * 诊断访问器？
 * @see  org.gradle.api.internal.file.FileOrUriNotationConverter
 */
public interface DiagnosticsVisitor {
    /**
     * Adds the description of some candidate.
     * 为某个 候选人添加描述？
     */
    DiagnosticsVisitor candidate(String displayName);

    /**
     * Adds an example for the previous candidate. Can have multiple examples.
     *
     * 添加一个样例
     */
    DiagnosticsVisitor example(String example);

    /**
     * Adds a set of potential values for the previous candidate, if known.
     * 添加一组诊断
     */
    DiagnosticsVisitor values(Iterable<?> values);
}

/*
 * Copyright 2011 the original author or authors.
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
package org.gradle.api.internal.file.collections;

import org.gradle.api.tasks.util.PatternFilterable;

/**
 * A file tree which can provide an efficient implementation for filtering using patterns.
 * 一个能够给提供高效的正则过滤的文件树
 */
public interface PatternFilterableFileTree extends MinimalFileTree {
    MinimalFileTree filter(PatternFilterable patterns);
}

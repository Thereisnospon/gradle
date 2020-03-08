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

/**
 * A minimal file collection. An implementation can optionally also implement the following interfaces:
 *
 * <ul>
 * <li>{@link org.gradle.api.Buildable}</li>
 * <li>{@link RandomAccessFileCollection}</li>
 * </ul>
 * 最小的文件集合。实现可能 也会实现上面两个
 */
public interface MinimalFileCollection {
    String getDisplayName();
}

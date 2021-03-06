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

import org.gradle.api.file.FileVisitor;
import org.gradle.api.internal.file.FileSystemSubset;

/**
 * A minimal file tree implementation. An implementation can optionally also implement the following interfaces:
 *
 * <ul>
 *  <li>{@link FileSystemMirroringFileTree}</li>
 *  <li>{@link LocalFileTree}</li>
 *  <li>{@link PatternFilterableFileTree}</li>
 * </ul>
 * 最小的文件树 实现。实现可能也会实现上面几个接口
 */
public interface MinimalFileTree extends MinimalFileCollection {
    /**
     * Visits the elements of this tree, in depth-first prefix order.
     */
    void visit(FileVisitor visitor);

    void registerWatchPoints(FileSystemSubset.Builder builder);

    /**
     * 访问后备文件（zip文件本身）或者 里面的 tree 内容
     * @param visitor
     */
    void visitTreeOrBackingFile(FileVisitor visitor);
}

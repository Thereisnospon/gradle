/*
 * Copyright 2016 the original author or authors.
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

package org.gradle.api.internal.file;

import org.gradle.api.tasks.util.PatternSet;

import java.io.File;

/**
 * Used with {@link FileCollectionInternal#visitLeafCollections(FileCollectionLeafVisitor)} this visitor
 * gets called for each element in a file collection that represents a root of a file tree.
 * @see org.gradle.api.internal.file.collections.FileTreeAdapter
 */
public interface FileCollectionLeafVisitor {
    /**
     * Visits a {@link FileCollectionInternal} element that cannot be visited in further detail.
     * 访问一个 FileCollectionInternal 元素，并且不能访问更多的细节。
     * @see org.gradle.api.internal.file.collections.FileTreeAdapter
     */
    void visitCollection(FileCollectionInternal fileCollection);

    /**
     * Visits a {@link FileTreeInternal} that does not represents a directory in the file system.
     * 访问一个不在文件系统上以 directory 形式表示的 FileTreeInternal (tar,zip?)
     * @see org.gradle.api.internal.file.collections.FileTreeAdapter
     */
    void visitGenericFileTree(FileTreeInternal fileTree);

    /**
     * Visits a file tree at a root file on the file system (potentially filtered).
     * 访问文件系统上 一个 file tree 的根目录，并且提供了定义该目录的过滤规则， visitor 遍历 root File 时可以参考
     * 过滤规则
     * @see org.gradle.api.internal.file.collections.FileTreeAdapter
     */
    void visitFileTree(File root, PatternSet patterns);
}

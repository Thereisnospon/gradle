/*
 * Copyright 2017 the original author or authors.
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

import org.gradle.api.GradleException;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.file.RelativePath;
import org.gradle.api.internal.file.DefaultFileVisitDetails;
import org.gradle.api.specs.Spec;
import org.gradle.internal.nativeintegration.filesystem.FileSystem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
//抽象的文件遍历器
public abstract class AbstractDirectoryWalker implements DirectoryWalker {
    private final FileSystem fileSystem;

    public AbstractDirectoryWalker(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    @Override
    public void walkDir(File file, RelativePath path, FileVisitor visitor, Spec<? super FileTreeElement> spec, AtomicBoolean stopFlag, boolean postfix) {
        File[] children = getChildren(file);
        if (children == null) {
            if (file.isDirectory() && !file.canRead()) {
                throw new GradleException(String.format("Could not list contents of directory '%s' as it is not readable.", file));
            }
            // else, might be a link which points to nothing, or has been removed while we're visiting, or ...
            throw new GradleException(String.format("Could not list contents of '%s'.", file));
        }
        List<FileVisitDetails> dirs = new ArrayList<FileVisitDetails>();
        for (int i = 0; !stopFlag.get() && i < children.length; i++) {
            File child = children[i];
            boolean isFile = child.isFile();
            RelativePath childPath = path.append(isFile, child.getName());
            FileVisitDetails details = new DefaultFileVisitDetails(child, childPath, stopFlag, fileSystem, fileSystem, !isFile);
            //spec.isSatisfiedBy(details)
            if (DirectoryFileTree.isAllowed(details, spec)) {
                if (isFile) {
                    visitor.visitFile(details);
                } else {
                    //先把同一层级的文件访问，目录暂时保存
                    dirs.add(details);
                }
            }
        }

        // now handle dirs
        //同一层级的文件访问完后，递归访问剩下的目录
        for (int i = 0; !stopFlag.get() && i < dirs.size(); i++) {
            FileVisitDetails dir = dirs.get(i);
            if (postfix) {
                //先寻找更深层次的再 访问
                walkDir(dir.getFile(), dir.getRelativePath(), visitor, spec, stopFlag, postfix);
                visitor.visitDir(dir);
            } else {
                //先访问 然后寻找更深层次的
                visitor.visitDir(dir);
                walkDir(dir.getFile(), dir.getRelativePath(), visitor, spec, stopFlag, postfix);
            }
        }
    }

    protected abstract File[] getChildren(File file);
}

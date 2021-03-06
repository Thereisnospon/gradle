/*
 * Copyright 2010 the original author or authors.
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

import org.gradle.api.file.DirectoryTree;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.file.RelativePath;
import org.gradle.api.file.ReproducibleFileVisitor;
import org.gradle.api.internal.file.DefaultFileVisitDetails;
import org.gradle.api.internal.file.FileSystemSubset;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.api.tasks.util.PatternSet;
import org.gradle.internal.Factory;
import org.gradle.internal.nativeintegration.filesystem.FileSystem;
import org.gradle.internal.nativeintegration.services.FileSystems;
import org.gradle.util.GUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Directory walker supporting {@link Spec}s for includes and excludes.
 * The file system is traversed in depth-first prefix order - all files in a directory will be
 * visited before any child directory is visited.
 *
 * A file or directory will only be visited if it matches all includes and no
 * excludes.
 * 目录文件树，层次遍历节点。 需要匹配 includes excludes 规则才会被 visit
 */
public class DirectoryFileTree implements MinimalFileTree, PatternFilterableFileTree, RandomAccessFileCollection, LocalFileTree, DirectoryTree {
    private static final Logger LOGGER = LoggerFactory.getLogger(DirectoryFileTree.class);
    private static final Factory<DirectoryWalker> DEFAULT_DIRECTORY_WALKER_FACTORY = new DefaultDirectoryWalkerFactory();
    private static final DirectoryWalker REPRODUCIBLE_DIRECTORY_WALKER = new ReproducibleDirectoryWalker(FileSystems.getDefault());

    private final File dir;
    private final PatternSet patternSet;
    private final boolean postfix;
    private final FileSystem fileSystem;
    private final Factory<DirectoryWalker> directoryWalkerFactory;

    public DirectoryFileTree(File dir, PatternSet patternSet, FileSystem fileSystem) {
        this(dir, patternSet, DEFAULT_DIRECTORY_WALKER_FACTORY, fileSystem, false);
    }

    DirectoryFileTree(File dir, PatternSet patternSet, Factory<DirectoryWalker> directoryWalkerFactory, FileSystem fileSystem, boolean postfix) {
        this.patternSet = patternSet;
        this.dir = dir;
        this.directoryWalkerFactory = directoryWalkerFactory;
        this.fileSystem = fileSystem;
        this.postfix = postfix;
    }

    public String getDisplayName() {
        String includes = patternSet.getIncludes().isEmpty() ? "" : String.format(" include %s", GUtil.toString(patternSet.getIncludes()));
        String excludes = patternSet.getExcludes().isEmpty() ? "" : String.format(" exclude %s", GUtil.toString(patternSet.getExcludes()));
        return String.format("directory '%s'%s%s", dir, includes, excludes);
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    public PatternSet getPatterns() {
        return patternSet;
    }

    public File getDir() {
        return dir;
    }

    public Collection<DirectoryFileTree> getLocalContents() {
        return Collections.singletonList(this);
    }

    public DirectoryFileTree filter(PatternFilterable patterns) {
        PatternSet patternSet = this.patternSet.intersect();
        patternSet.copyFrom(patterns);
        return new DirectoryFileTree(dir, patternSet, directoryWalkerFactory, fileSystem, postfix);
    }

    public boolean contains(File file) {
        return DirectoryTrees.contains(fileSystem, this, file) && file.isFile();
    }

    @Override
    public void registerWatchPoints(FileSystemSubset.Builder builder) {
        builder.add(this);
    }

    @Override
    public void visitTreeOrBackingFile(FileVisitor visitor) {
        visit(visitor);
    }

    public void visit(FileVisitor visitor) {
        visitFrom(visitor, dir, RelativePath.EMPTY_ROOT);
    }

    /**
     * Process the specified file or directory.  If it is a directory, then its contents
     * (but not the directory itself) will be checked with {@link #isAllowed(FileTreeElement, Spec)} and notified to
     * the listener.  If it is a file, the file will be checked and notified.
     * 处理文件/目录。
     * 如果是目录，那么它的内容(不包括目录本身） 会被 isAllowed(FileTreeElement, Spec) 检查且通知 listener
     * 如果是文件，那么文件被检查且通知到 listener
     */
    public void visitFrom(FileVisitor visitor, File fileOrDirectory, RelativePath path) {
        AtomicBoolean stopFlag = new AtomicBoolean();
        //patternSet 转换成 Spec
        Spec<FileTreeElement> spec = patternSet.getAsSpec();
        if (fileOrDirectory.exists()) {
            if (fileOrDirectory.isFile()) {
                //单独的文件
                processSingleFile(fileOrDirectory, visitor, spec, stopFlag);
            } else {
                //walkDir 递归遍历目录内容
                walkDir(fileOrDirectory, path, visitor, spec, stopFlag);
            }
        } else {
            LOGGER.info("file or directory '{}', not found", fileOrDirectory);
        }
    }

    private void processSingleFile(File file, FileVisitor visitor, Spec<FileTreeElement> spec, AtomicBoolean stopFlag) {
        RelativePath path = new RelativePath(true, file.getName());
        FileVisitDetails details = new DefaultFileVisitDetails(file, path, stopFlag, fileSystem, fileSystem, false);
        //处理单独的文件，需要接受规则检查
        if (isAllowed(details, spec)) {
            visitor.visitFile(details);
        }
    }

    private void walkDir(File file, RelativePath path, FileVisitor visitor, Spec<FileTreeElement> spec, AtomicBoolean stopFlag) {
        DirectoryWalker directoryWalker;
        if (visitor instanceof ReproducibleFileVisitor && ((ReproducibleFileVisitor) visitor).isReproducibleFileOrder()) {
            directoryWalker = REPRODUCIBLE_DIRECTORY_WALKER;
        } else {
            directoryWalker = directoryWalkerFactory.create();
        }
        directoryWalker.walkDir(file, path, visitor, spec, stopFlag, postfix);
    }

    static boolean isAllowed(FileTreeElement element, Spec<? super FileTreeElement> spec) {
        return spec.isSatisfiedBy(element);
    }

    /**
     * Returns a copy that traverses directories (but not files) in postfix rather than prefix order.
     * 拷贝一个深层次优先遍历的 tree
     * @return {@code this}
     */
    public DirectoryFileTree postfix() {
        if (postfix) {
            return this;
        }
        return new DirectoryFileTree(dir, patternSet, directoryWalkerFactory, fileSystem, true);
    }

    public PatternSet getPatternSet() {
        return patternSet;
    }

}

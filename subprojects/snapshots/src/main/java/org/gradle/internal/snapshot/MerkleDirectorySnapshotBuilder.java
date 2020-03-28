/*
 * Copyright 2018 the original author or authors.
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

package org.gradle.internal.snapshot;

import org.gradle.internal.hash.HashCode;
import org.gradle.internal.hash.Hasher;
import org.gradle.internal.hash.Hashing;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * 按目录层级计算 目录 hash
 */
public class MerkleDirectorySnapshotBuilder implements FileSystemSnapshotVisitor {
    private static final HashCode DIR_SIGNATURE = Hashing.signature("DIR");
    //TODO 为啥要用 relativePathSegmentsTracker 判断 root ?
    private final RelativePathSegmentsTracker relativePathSegmentsTracker = new RelativePathSegmentsTracker();
    //按照目录由上到下 的层级记录没一层目录的子文件 snapshot
    private final Deque<List<FileSystemLocationSnapshot>> levelHolder = new ArrayDeque<List<FileSystemLocationSnapshot>>();
    private final Deque<String> directoryAbsolutePaths = new ArrayDeque<String>();
    private final boolean sortingRequired;
    private FileSystemLocationSnapshot result;
    //计算 content hash (child) 时需要排序
    public static MerkleDirectorySnapshotBuilder sortingRequired() {
        return new MerkleDirectorySnapshotBuilder(true);
    }

    public static MerkleDirectorySnapshotBuilder noSortingRequired() {
        return new MerkleDirectorySnapshotBuilder(false);
    }

    private MerkleDirectorySnapshotBuilder(boolean sortingRequired) {
        this.sortingRequired = sortingRequired;
    }

    public boolean preVisitDirectory(String absolutePath, String name) {
        relativePathSegmentsTracker.enter(name);
        levelHolder.addLast(new ArrayList<FileSystemLocationSnapshot>());
        directoryAbsolutePaths.addLast(absolutePath);
        return true;
    }

    @Override
    public boolean preVisitDirectory(DirectorySnapshot directorySnapshot) {
        return preVisitDirectory(directorySnapshot.getAbsolutePath(), directorySnapshot.getName());
    }

    @Override
    public void visit(FileSystemLocationSnapshot fileSnapshot) {
        if (relativePathSegmentsTracker.isRoot()) {
            result = fileSnapshot;
        } else {
            levelHolder.peekLast().add(fileSnapshot);
        }
    }

    @Override
    public void postVisitDirectory(DirectorySnapshot directorySnapshot) {
        postVisitDirectory(true);
    }

    public void postVisitDirectory() {
        postVisitDirectory(true);
    }

    public boolean postVisitDirectory(boolean includeEmpty) {
        String name = relativePathSegmentsTracker.leave();
        //该目录对应那一层的被移除
        List<FileSystemLocationSnapshot> children = levelHolder.removeLast();
        String absolutePath = directoryAbsolutePaths.removeLast();
        if (children.isEmpty() && !includeEmpty) {
            return false;
        }
        if (sortingRequired) {
            Collections.sort(children, FileSystemLocationSnapshot.BY_NAME);
        }
        Hasher hasher = Hashing.newHasher();
        //这里如果是空目录的话 也会被计算 hash. 如果是
        //1. a/b  b 为空目录
        //2. c/
        //3. 那么 a 的 hash 计算与 c 不同？
        hasher.putHash(DIR_SIGNATURE);
        for (FileSystemLocationSnapshot child : children) {
            hasher.putString(child.getName());
            hasher.putHash(child.getHash());
        }
        DirectorySnapshot directorySnapshot = new DirectorySnapshot(absolutePath, name, children, hasher.hash());
        //该目录对应一层的被移除了，那么 last 是该目录父目录的 子文件列表，也就是它的兄弟列表
        List<FileSystemLocationSnapshot> siblings = levelHolder.peekLast();
        if (siblings != null) {
            //有兄弟列表，那么说明不是 root ，那么将自己的 snapshot 加入到兄弟列表
            siblings.add(directorySnapshot);
        } else {
            //root 节点的
            result = directorySnapshot;
        }
        return true;
    }

    public boolean isRoot() {
        return relativePathSegmentsTracker.isRoot();
    }

    public Iterable<String> getRelativePath() {
        return relativePathSegmentsTracker.getRelativePath();
    }

    @Nullable
    public FileSystemLocationSnapshot getResult() {
        return result;
    }
}

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

package org.gradle.internal.fingerprint.classpath.impl;

import org.gradle.internal.change.ChangeVisitor;
import org.gradle.internal.change.FileChange;
import org.gradle.internal.fingerprint.FileCollectionFingerprint;
import org.gradle.internal.fingerprint.FileSystemLocationFingerprint;
import org.gradle.internal.fingerprint.FingerprintCompareStrategy;
import org.gradle.internal.fingerprint.impl.AbstractFingerprintCompareStrategy;
import org.gradle.internal.hash.Hasher;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Compares two {@link FileCollectionFingerprint}s representing classpaths.
 *
 * That means that the comparison happens in-order with relative path sensitivity.
 */
public class ClasspathCompareStrategy extends AbstractFingerprintCompareStrategy {
    public static final FingerprintCompareStrategy INSTANCE = new ClasspathCompareStrategy();

    private ClasspathCompareStrategy() {
    }
    //需要 在同一个位置上(同一个位置(?? 如果 visitor 一直 返回 true 貌似就错位了?)) 一一比较
    @Override
    protected boolean doVisitChangesSince(ChangeVisitor visitor, Map<String, FileSystemLocationFingerprint> currentSnapshots, Map<String, FileSystemLocationFingerprint> previousSnapshots, String propertyTitle, boolean includeAdded) {
        Iterator<Map.Entry<String, FileSystemLocationFingerprint>> currentEntries = currentSnapshots.entrySet().iterator();
        Iterator<Map.Entry<String, FileSystemLocationFingerprint>> previousEntries = previousSnapshots.entrySet().iterator();
        while (true) {
            if (currentEntries.hasNext()) {
                Map.Entry<String, FileSystemLocationFingerprint> current = currentEntries.next();
                String currentAbsolutePath = current.getKey();
                if (previousEntries.hasNext()) {
                    Map.Entry<String, FileSystemLocationFingerprint> previous = previousEntries.next();
                    FileSystemLocationFingerprint currentFingerprint = current.getValue();
                    FileSystemLocationFingerprint previousFingerprint = previous.getValue();
                    String currentNormalizedPath = currentFingerprint.getNormalizedPath();
                    String previousNormalizedPath = previousFingerprint.getNormalizedPath();
                    if (currentNormalizedPath.equals(previousNormalizedPath)) {
                        //内容不一致, modify
                        if (!currentFingerprint.getNormalizedContentHash().equals(previousFingerprint.getNormalizedContentHash())) {
                            if (!visitor.visitChange(
                                FileChange.modified(currentAbsolutePath, propertyTitle,
                                    previousFingerprint.getType(),
                                    currentFingerprint.getType()
                                ))) {
                                return false;
                            }
                        }
                        //内容路径都相同
                    } else {
                        //路径不一致
                        String previousAbsolutePath = previous.getKey();
                        //同一个位置(?? 如果 visitor 一直 返回 true 貌似就错位了?)的 current 和 pre 内容不一致. 记录为
                        // 1. pre 被移除
                        // 2. current 被添加
                        if (!visitor.visitChange(FileChange.removed(previousAbsolutePath, propertyTitle, previousFingerprint.getType()))) {
                            return false;
                        }
                        if (includeAdded) {
                            //指定考虑 add的变化
                            if (!visitor.visitChange(FileChange.added(currentAbsolutePath, propertyTitle, currentFingerprint.getType()))) {
                                return false;
                            }
                        }
                    }
                } else {
                    //同一个位置(?? 如果 visitor 一直 返回 true 貌似就错位了?) 的 current 有，而 pre 没有。那么 current 被add了
                    if (includeAdded) {
                        if (!visitor.visitChange(FileChange.added(currentAbsolutePath, propertyTitle, current.getValue().getType()))) {
                            return false;
                        }
                    }
                }
            } else {
                //当前文件遍历结束，如果原来的还有，那么 pre 都被移除了
                if (previousEntries.hasNext()) {
                    Map.Entry<String, FileSystemLocationFingerprint> previousEntry = previousEntries.next();
                    if (!visitor.visitChange(FileChange.removed(previousEntry.getKey(), propertyTitle, previousEntry.getValue().getType()))) {
                        return false;
                    }
                } else {
                    return true;
                }
            }
        }
    }

    @Override
    public void appendToHasher(Hasher hasher, Collection<FileSystemLocationFingerprint> fingerprints) {
        //TODO 。这里貌似没有排序后进行 计算
        for (FileSystemLocationFingerprint fingerprint : fingerprints) {
            fingerprint.appendToHasher(hasher);
        }
    }
}

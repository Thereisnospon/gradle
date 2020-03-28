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

package org.gradle.internal.fingerprint.impl;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.MultimapBuilder;
import org.gradle.internal.change.ChangeVisitor;
import org.gradle.internal.change.FileChange;
import org.gradle.internal.fingerprint.FileCollectionFingerprint;
import org.gradle.internal.fingerprint.FileSystemLocationFingerprint;
import org.gradle.internal.fingerprint.FingerprintCompareStrategy;
import org.gradle.internal.hash.HashCode;
import org.gradle.internal.hash.Hasher;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Compares {@link FileCollectionFingerprint}s ignoring the path.
 * 忽略文件路径，只对内容进行比较
 */
public class IgnoredPathCompareStrategy extends AbstractFingerprintCompareStrategy {
    public static final FingerprintCompareStrategy INSTANCE = new IgnoredPathCompareStrategy();
    ///根据hash值比较
    private static final Comparator<Map.Entry<HashCode, FilePathWithType>> ENTRY_COMPARATOR = new Comparator<Map.Entry<HashCode, FilePathWithType>>() {
        @Override
        public int compare(Map.Entry<HashCode, FilePathWithType> o1, Map.Entry<HashCode, FilePathWithType> o2) {
            return o1.getKey().compareTo(o2.getKey());
        }
    };

    private IgnoredPathCompareStrategy() {
    }

    /**
     * Determines changes by:
     *
     * <ul>
     *     <li>Determining which content fingerprints are only in the previous or current fingerprint collection.</li>
     *     <li>Those only in the previous fingerprint collection are reported as removed.</li>
     *     <li>If {@code includeAdded} is {@code true}, the files with content fingerprints which are only in the current collection are reported as added.</li>
     * </ul>
     */
    @Override
    protected boolean doVisitChangesSince(ChangeVisitor visitor, Map<String, FileSystemLocationFingerprint> current, Map<String, FileSystemLocationFingerprint> previous, String propertyTitle, boolean includeAdded) {
        //保存同 hash key 的所有文件
        ListMultimap<HashCode, FilePathWithType> unaccountedForPreviousFiles = MultimapBuilder.hashKeys(previous.size()).linkedListValues().build();
        for (Map.Entry<String, FileSystemLocationFingerprint> entry : previous.entrySet()) {
            String absolutePath = entry.getKey();
            FileSystemLocationFingerprint previousFingerprint = entry.getValue();
            unaccountedForPreviousFiles.put(previousFingerprint.getNormalizedContentHash(), new FilePathWithType(absolutePath, previousFingerprint.getType()));
        }

        for (Map.Entry<String, FileSystemLocationFingerprint> entry : current.entrySet()) {
            String currentAbsolutePath = entry.getKey();
            FileSystemLocationFingerprint currentFingerprint = entry.getValue();
            HashCode normalizedContentHash = currentFingerprint.getNormalizedContentHash();
            List<FilePathWithType> previousFilesForContent = unaccountedForPreviousFiles.get(normalizedContentHash);
            //如果没有相同hash 的文件
            if (previousFilesForContent.isEmpty()) {
                //所有当前文件都是被 add ，根据需要 visit
                if (includeAdded) {
                    if (!visitor.visitChange(FileChange.added(currentAbsolutePath, propertyTitle, currentFingerprint.getType()))) {
                        return false;
                    }
                }
            } else {
                //有相同 hash 的文件，移除第一个？可能相同 hash 的会被一个一个的移除掉，这个是以1比1 移除的流程，如果遍历完了现有文件，还剩的话，那剩下的都是被移除的
                previousFilesForContent.remove(0);
            }
        }

        List<Map.Entry<HashCode, FilePathWithType>> unaccountedForPreviousEntries = Lists.newArrayList(unaccountedForPreviousFiles.entries());
        //根据hash值排序
        Collections.sort(unaccountedForPreviousEntries, ENTRY_COMPARATOR);
        for (Map.Entry<HashCode, FilePathWithType> unaccountedForPreviousEntry : unaccountedForPreviousEntries) {
            FilePathWithType removedFile = unaccountedForPreviousEntry.getValue();
            //参考上面，剩下的都是被移除的
            if (!visitor.visitChange(FileChange.removed(removedFile.getAbsolutePath(), propertyTitle, removedFile.getFileType()))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void appendToHasher(Hasher hasher, Collection<FileSystemLocationFingerprint> fingerprints) {
        //排序后比较
        NormalizedPathFingerprintCompareStrategy.appendSortedToHasher(hasher, fingerprints);
    }
}

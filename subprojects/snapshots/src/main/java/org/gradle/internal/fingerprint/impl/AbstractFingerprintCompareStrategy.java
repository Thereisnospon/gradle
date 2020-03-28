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

import com.google.common.annotations.VisibleForTesting;
import org.gradle.internal.change.Change;
import org.gradle.internal.change.ChangeVisitor;
import org.gradle.internal.change.FileChange;
import org.gradle.internal.fingerprint.FileSystemLocationFingerprint;
import org.gradle.internal.fingerprint.FingerprintCompareStrategy;
import org.gradle.internal.hash.HashCode;

import javax.annotation.Nullable;
import java.util.Map;

public abstract class AbstractFingerprintCompareStrategy implements FingerprintCompareStrategy {

    @Override
    public boolean visitChangesSince(ChangeVisitor visitor, Map<String, FileSystemLocationFingerprint> current, Map<String, FileSystemLocationFingerprint> previous, String propertyTitle, boolean includeAdded) {
        // Handle trivial cases with 0 or 1 elements in both current and previous
        //检查一些简单的变更
        Boolean trivialResult = compareTrivialFingerprints(visitor, current, previous, propertyTitle, includeAdded);
        if (trivialResult != null) {
            return trivialResult;
        }
        //执行复杂的变更检查
        return doVisitChangesSince(visitor, current, previous, propertyTitle, includeAdded);
    }

    protected abstract boolean doVisitChangesSince(ChangeVisitor visitor, Map<String, FileSystemLocationFingerprint> current, Map<String, FileSystemLocationFingerprint> previous, String propertyTitle, boolean includeAdded);

    /**
     * Compares collection fingerprints if one of current or previous are empty or both have at most one element.
     *
     * @return {@code null} if the comparison is not trivial.
     * For a trivial comparision returns whether the {@link ChangeVisitor} is looking for further changes.
     * See {@link ChangeVisitor#visitChange(Change)}.
     *   比较两个 fingerprints，如果都为空，或者两个中最多只有1个元素
     */
    @VisibleForTesting
    @Nullable
    static Boolean compareTrivialFingerprints(ChangeVisitor visitor, Map<String, FileSystemLocationFingerprint> current, Map<String, FileSystemLocationFingerprint> previous, String propertyTitle, boolean includeAdded) {
        switch (current.size()) {
            case 0:
                switch (previous.size()) {
                    case 0:
                        return true;
                    default:
                        for (Map.Entry<String, FileSystemLocationFingerprint> entry : previous.entrySet()) {
                            //current 一个都没有，而原先的有，那么原先的全都是被移除了
                            Change change = FileChange.removed(entry.getKey(), propertyTitle, entry.getValue().getType());
                            //visitor 不然想访问更多的 change
                            if (!visitor.visitChange(change)) {
                                return false;
                            }
                        }
                        return true;
                }

            case 1:
                switch (previous.size()) {
                    case 0:
                        //原来没文件，现在有1个文件，报告为 add
                        return reportAllAdded(visitor, current, propertyTitle, includeAdded);
                    case 1:
                        Map.Entry<String, FileSystemLocationFingerprint> previousEntry = previous.entrySet().iterator().next();
                        Map.Entry<String, FileSystemLocationFingerprint> currentEntry = current.entrySet().iterator().next();
                        return compareTrivialFingerprintEntries(visitor, currentEntry, previousEntry, propertyTitle, includeAdded);
                    default:
                        //现在有1个文件，而原来有多个文件，不继续轻量查找
                        return null;
                }

            default:
                //现在有多个文件，原来的文件又不为空，那么不继续轻量的查找
                if (!previous.isEmpty()) {
                    return null;
                }
                //原来没有文件，现在有多个文件，那么现在的都是 add 的
                return reportAllAdded(visitor, current, propertyTitle, includeAdded);
        }
    }

    private static boolean reportAllAdded(ChangeVisitor visitor, Map<String, FileSystemLocationFingerprint> current, String propertyTitle, boolean includeAdded) {
        if (includeAdded) {
            for (Map.Entry<String, FileSystemLocationFingerprint> entry : current.entrySet()) {
                Change change = FileChange.added(entry.getKey(), propertyTitle, entry.getValue().getType());
                if (!visitor.visitChange(change)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean compareTrivialFingerprintEntries(ChangeVisitor visitor, Map.Entry<String, FileSystemLocationFingerprint> currentEntry, Map.Entry<String, FileSystemLocationFingerprint> previousEntry, String propertyTitle, boolean includeAdded) {
        FileSystemLocationFingerprint previousFingerprint = previousEntry.getValue();
        FileSystemLocationFingerprint currentFingerprint = currentEntry.getValue();
        if (currentFingerprint.getNormalizedPath().equals(previousFingerprint.getNormalizedPath())) {
            //路径一样 比较 contentHash
            HashCode previousContent = previousFingerprint.getNormalizedContentHash();
            HashCode currentContent = currentFingerprint.getNormalizedContentHash();
            if (!currentContent.equals(previousContent)) {
                String path = currentEntry.getKey();
                //内容变更
                Change change = FileChange.modified(path, propertyTitle, previousFingerprint.getType(), currentFingerprint.getType());
                return visitor.visitChange(change);
            }
            return true;
        } else {
            String previousPath = previousEntry.getKey();
            // 原来只有文件 A ,现在只有文件 B ，那么有两个 change
            // 1. A 被 remove
            // 2. B 被 add
            Change remove = FileChange.removed(previousPath, propertyTitle, previousFingerprint.getType());
            //是否检测 add 的变化
            if (includeAdded) {
                String currentPath = currentEntry.getKey();
                Change add = FileChange.added(currentPath, propertyTitle, currentFingerprint.getType());
                // remove 和 add 都被要求继续访问
                return visitor.visitChange(remove) && visitor.visitChange(add);
            } else {
                //只访问 remove 的变化
                return visitor.visitChange(remove);
            }
        }
    }
}

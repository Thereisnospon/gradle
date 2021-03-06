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

package org.gradle.internal.fingerprint.impl;

import org.gradle.api.NonNullApi;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.cache.StringInterner;
import org.gradle.api.internal.file.FileCollectionInternal;
import org.gradle.internal.fingerprint.CurrentFileCollectionFingerprint;
import org.gradle.internal.fingerprint.FileCollectionFingerprint;
import org.gradle.internal.fingerprint.FileCollectionFingerprinter;
import org.gradle.internal.fingerprint.FingerprintingStrategy;
import org.gradle.internal.snapshot.FileSystemSnapshot;
import org.gradle.internal.snapshot.FileSystemSnapshotter;

import java.util.List;

/**
 * Responsible for calculating a {@link FileCollectionFingerprint} for a particular {@link FileCollection}.
 * 负责计算一个 FileCollection 的 FileCollectionFingerprint
 */
@NonNullApi
public abstract class AbstractFileCollectionFingerprinter implements FileCollectionFingerprinter {
    private final StringInterner stringInterner;
    private final FileSystemSnapshotter fileSystemSnapshotter;

    public AbstractFileCollectionFingerprinter(StringInterner stringInterner, FileSystemSnapshotter fileSystemSnapshotter) {
        this.stringInterner = stringInterner;
        this.fileSystemSnapshotter = fileSystemSnapshotter;
    }

    public CurrentFileCollectionFingerprint fingerprint(FileCollection input, FingerprintingStrategy strategy) {
        FileCollectionInternal fileCollection = (FileCollectionInternal) input;
        List<FileSystemSnapshot> roots = fileSystemSnapshotter.snapshot(fileCollection);
        //通过 snapshot 计算文件指纹
        return DefaultCurrentFileCollectionFingerprint.from(roots, strategy);
    }

    protected StringInterner getStringInterner() {
        return stringInterner;
    }
}

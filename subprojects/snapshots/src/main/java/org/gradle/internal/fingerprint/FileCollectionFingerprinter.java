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
package org.gradle.internal.fingerprint;

import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.FileNormalizer;

public interface FileCollectionFingerprinter {
    /**
     * The type used to refer to this fingerprinter in the {@link FileCollectionFingerprinterRegistry}.
     * 用于在 FileCollectionFingerprinterRegistry 中注册用的 type
     */
    Class<? extends FileNormalizer> getRegisteredType();

    /**
     * Creates a fingerprint of the contents of the given collection.
     * 给文件集的内容创建指纹
     */
    CurrentFileCollectionFingerprint fingerprint(FileCollection files);

    /**
     * Returns an empty fingerprint.
     * 创建空的文件集指纹
     */
    CurrentFileCollectionFingerprint empty();
}

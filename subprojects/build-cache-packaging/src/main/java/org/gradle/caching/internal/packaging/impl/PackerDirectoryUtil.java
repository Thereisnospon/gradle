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

package org.gradle.caching.internal.packaging.impl;

import org.apache.commons.io.FileUtils;
import org.gradle.internal.file.TreeType;

import java.io.File;
import java.io.IOException;

public class PackerDirectoryUtil {
    //确保文件的目录存在，如果 root 是目录，不存在该目录则创建该目录， 如果 root 是 file, 那么 parent 目录不存在则创建
    public static void ensureDirectoryForTree(TreeType type, File root) throws IOException {
        switch (type) {
            case DIRECTORY:
                if (!makeDirectory(root)) {
                    FileUtils.cleanDirectory(root);
                }
                break;
            case FILE:
                if (!makeDirectory(root.getParentFile())) {
                    if (root.exists()) {
                        FileUtils.forceDelete(root);
                    }
                }
                break;
            default:
                throw new AssertionError();
        }
    }

    public static boolean makeDirectory(File target) throws IOException {
        if (target.isDirectory()) {
            return false;
        } else if (target.isFile()) {
            FileUtils.forceDelete(target);
        }
        FileUtils.forceMkdir(target);
        return true;
    }
}

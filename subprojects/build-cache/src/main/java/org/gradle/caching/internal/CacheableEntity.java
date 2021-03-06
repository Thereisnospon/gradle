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

package org.gradle.caching.internal;

import org.gradle.api.Describable;
import org.gradle.internal.file.TreeType;

import javax.annotation.Nullable;
import java.io.File;

/**
 * An entity that can potentially be stored in the build cache.
 * 能被 通过 build-cache 存储的元素
 */
public interface CacheableEntity extends Describable {
    String getIdentity();

    void visitTrees(CacheableTreeVisitor visitor);

    interface CacheableTreeVisitor {
        void visitTree(String name, TreeType type, @Nullable File root);
    }
}

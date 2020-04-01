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

package org.gradle.api.internal;

import org.gradle.internal.classpath.ClassPath;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DefaultClassPathRegistry implements ClassPathRegistry {
    private final List<ClassPathProvider> providers = new ArrayList<ClassPathProvider>();

    public DefaultClassPathRegistry(ClassPathProvider... providers) {
        this.providers.addAll(Arrays.asList(providers));
    }
    //委托 ClassPathProvider 查找
    public ClassPath getClassPath(String name) {
        for (ClassPathProvider provider : providers) {
            ClassPath classpath = provider.findClassPath(name);
            if (classpath != null) {
                return classpath;
            }
        }
        throw new IllegalArgumentException(String.format("unknown classpath '%s' requested.", name));
    }
}

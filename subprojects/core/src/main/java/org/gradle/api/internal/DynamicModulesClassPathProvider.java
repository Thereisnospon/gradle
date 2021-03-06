/*
 * Copyright 2011 the original author or authors.
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

import com.google.common.annotations.VisibleForTesting;
import org.gradle.api.JavaVersion;
import org.gradle.api.internal.classpath.Module;
import org.gradle.api.internal.classpath.ModuleRegistry;
import org.gradle.api.internal.classpath.PluginModuleRegistry;
import org.gradle.api.specs.Spec;
import org.gradle.internal.classpath.ClassPath;

import java.io.File;
import java.util.Set;

import static java.util.Collections.emptySet;

public class DynamicModulesClassPathProvider implements ClassPathProvider {
    private final ModuleRegistry moduleRegistry;
    private final PluginModuleRegistry pluginModuleRegistry;
    private final JavaVersion javaVersion;

    public DynamicModulesClassPathProvider(ModuleRegistry moduleRegistry, PluginModuleRegistry pluginModuleRegistry) {
        this(moduleRegistry, pluginModuleRegistry, JavaVersion.current());
    }

    @VisibleForTesting
    protected DynamicModulesClassPathProvider(ModuleRegistry moduleRegistry, PluginModuleRegistry pluginModuleRegistry, JavaVersion javaVersion) {
        this.moduleRegistry = moduleRegistry;
        this.pluginModuleRegistry = pluginModuleRegistry;
        this.javaVersion = javaVersion;
    }

    public ClassPath findClassPath(String name) {
        //限定了 GRADLE_EXTENSIONS
        if (name.equals("GRADLE_EXTENSIONS")) {
            //gradle-core classpath.properties 定义的依赖的普通 module
            Set<Module> coreModules = allRequiredModulesOf("gradle-core");
            ClassPath classpath = ClassPath.EMPTY;
            for (String moduleName : GRADLE_EXTENSION_MODULES) {
                Set<Module> extensionModules = allRequiredModulesOf(moduleName);
                //需要跟 core 中已有依赖判断
                classpath = plusExtensionModules(classpath, extensionModules, coreModules);
            }
            for (String moduleName : GRADLE_OPTIONAL_EXTENSION_MODULES) {
                Set<Module> optionalExtensionModules = allRequiredModulesOfOptional(moduleName);
                classpath = plusExtensionModules(classpath, optionalExtensionModules, coreModules);
            }
            //gradle-plugin.properties 和 gradle-implementation-plugin.properties 定义的 插件 module
            for (Module pluginModule : pluginModuleRegistry.getApiModules()) {
                classpath = classpath.plus(pluginModule.getClasspath());
            }
            for (Module pluginModule : pluginModuleRegistry.getImplementationModules()) {
                classpath = classpath.plus(pluginModule.getClasspath());
            }
            return removeJaxbIfIncludedInCurrentJdk(classpath);
        }

        return null;
    }

    private ClassPath removeJaxbIfIncludedInCurrentJdk(ClassPath classpath) {
        if (!javaVersion.isJava9Compatible()) {
            return classpath.removeIf(new Spec<File>() {
                @Override
                public boolean isSatisfiedBy(File file) {
                    return file.getName().startsWith("jaxb-impl-");
                }
            });
        }
        return classpath;
    }

    private Set<Module> allRequiredModulesOf(String name) {
        return moduleRegistry.getModule(name).getAllRequiredModules();
    }

    private Set<Module> allRequiredModulesOfOptional(String moduleName) {
        Module optionalModule = moduleRegistry.findModule(moduleName);
        if (optionalModule != null) {
            return optionalModule.getAllRequiredModules();
        }
        return emptySet();
    }

    private ClassPath plusExtensionModules(ClassPath classpath, Set<Module> extensionModules, Set<Module> coreModules) {
        for (Module module : extensionModules) {
            if (!coreModules.contains(module)) {
                classpath = classpath.plus(module.getClasspath());
            }
        }
        return classpath;
    }

    private static final String[] GRADLE_EXTENSION_MODULES = {
        "gradle-workers",
        "gradle-dependency-management",
        "gradle-plugin-use"
    };

    private static final String[] GRADLE_OPTIONAL_EXTENSION_MODULES = {
        "gradle-kotlin-dsl-provider-plugins",
        "gradle-kotlin-dsl-tooling-builders"
    };
}

import org.gradle.gradlebuild.testing.integrationtests.cleanup.WhenNotEmpty
import org.gradle.gradlebuild.unittestandcompile.ModuleType

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
plugins {
    `java-library`
}
apply {
    from("${project.rootDir}/gradle/useTestDistribution.gradle")
}
dependencies {
    implementation(project(":baseServices"))
    implementation(project(":logging"))
    implementation(project(":processServices"))
    implementation(project(":fileCollections"))
    implementation(project(":persistentCache"))
    implementation(project(":coreApi"))
    implementation(project(":modelCore"))
    implementation(project(":core"))
    implementation(project(":workers"))
    implementation(project(":dependencyManagement"))
    implementation(project(":reporting"))
    implementation(project(":platformBase"))
    implementation(project(":platformJvm"))
    implementation(project(":languageJvm"))
    implementation(project(":languageJava"))
    implementation(project(":languageGroovy"))
    implementation(project(":diagnostics"))
    implementation(project(":testingBase"))
    implementation(project(":testingJvm"))
    implementation(project(":snapshots"))

    implementation(library("slf4j_api"))
    implementation(library("groovy"))
    implementation(library("ant"))
    implementation(library("asm"))
    implementation(library("guava"))
    implementation(library("commons_io"))
    implementation(library("commons_lang"))
    implementation(library("inject"))

    // This dependency makes the services provided by `:compositeBuilds` available at runtime for all integration tests in all subprojects
    // Making this better would likely involve a separate `:gradleRuntime` module that brings in `:core`, `:dependencyManagement` and other key subprojects
    runtimeOnly(project(":compositeBuilds"))

    testImplementation(project(":messaging"))
    testImplementation(project(":native"))
    testImplementation(project(":resources"))
    testImplementation(testFixtures(project(":core")))
    testImplementation(testFixtures(project(":launcher")))
    testImplementation(testFixtures(project(":dependencyManagement")))
    testImplementation(testFixtures(project(":resourcesHttp")))
    testImplementation(testFixtures(project(":platformNative")))
    testImplementation(testFixtures(project(":languageJvm")))
    testImplementation(testFixtures(project(":languageJava")))
    testImplementation(testFixtures(project(":languageGroovy")))
    testImplementation(testFixtures(project(":diagnostics")))

    testRuntimeOnly(project(":runtimeApiInfo"))

    testFixturesImplementation(testFixtures(project(":core")))
    testFixturesImplementation(project(":baseServicesGroovy"))
    testFixturesImplementation(project(":fileCollections"))
    testFixturesImplementation(project(":languageJvm"))
    testFixturesImplementation(project(":internalIntegTesting"))
    testFixturesImplementation(library("guava"))

    testImplementation(testLibrary("jsoup"))

    integTestRuntimeOnly(project(":maven"))

    testImplementation(library("gson")) {
        because("for unknown reason (bug in the Groovy/Spock compiler?) requires it to be present to use the Gradle Module Metadata test fixtures")
    }
    integTestRuntimeOnly(project(":testingJunitPlatform"))
}


gradlebuildJava {
    moduleType = ModuleType.CORE
}

val wrapperJarDir = file("$buildDir/generated-resources/wrapper-jar")
evaluationDependsOn(":wrapper")
val wrapperJar by tasks.registering(Copy::class) {
    from(project(":wrapper").tasks.named("executableJar"))
    into(wrapperJarDir)
}
sourceSets.main {
    output.dir(wrapperJarDir, "builtBy" to wrapperJar)
}

testFilesCleanup {
    policy.set(WhenNotEmpty.REPORT)
}

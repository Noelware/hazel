/*
 * 🪶 Hazel: Minimal, and fast HTTP proxy to host files from any cloud storage provider.
 * Copyright 2022-2023 Noelware <team@noelware.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.noelware.hazel.gradle.*
import dev.floofy.utils.gradle.*

plugins {
    id("com.diffplug.spotless")
    `java-library`
    java
}

group = "org.noelware.hazel"
version = "$VERSION"

repositories {
    mavenCentral()
}

dependencies {
    // jetbrains annotations
    implementation("org.jetbrains:annotations:23.1.0")

    // common stuff
    implementation(project(":common"))

    // logback modules
    implementation(libs.logback.logstash)
    implementation(libs.logback.classic)
    implementation(libs.sentry.logback)
    implementation(libs.logback.core)
}

spotless {
    java {
        licenseHeaderFile("${rootProject.projectDir}/assets/HEADING")
        trimTrailingWhitespace()
        removeUnusedImports()
        palantirJavaFormat()
        endWithNewline()
    }
}

java {
    sourceCompatibility = JAVA_VERSION
    targetCompatibility = JAVA_VERSION
}

tasks {
    withType<Jar> {
        archiveFileName by "hazel-logging-$VERSION.jar"
        manifest {
            attributes(
                mapOf(
                    "Implementation-Version" to "$VERSION",
                    "Implementation-Vendor" to "Noelware, LLC. [team@noelware.org]",
                    "Implementation-Title" to "Hazel"
                )
            )
        }
    }
}

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

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.JavaVersion

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
}

repositories {
    maven("https://maven.floofy.dev/repo/releases")
    gradlePluginPortal()
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.18.5")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:6.12.0")
    implementation("com.netflix.nebula:gradle-ospackage-plugin:11.3.0")
    implementation("dev.floofy.commons:gradle:2.5.1")
    implementation(kotlin("serialization", "1.7.22"))
    implementation(kotlin("gradle-plugin", "1.7.22"))
}

gradlePlugin {
    plugins {
        create("nebula") {
            id = "org.noelware.hazel.dist.nebula"
            implementationClass = "org.noelware.hazel.gradle.plugins.HazelNebulaPlugin"
        }
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
}

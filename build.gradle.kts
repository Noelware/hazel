/*
 * 🪶 hazel: Minimal, simple, and open source content delivery network made in Kotlin
 * Copyright 2022 Noel <cutie@floofy.dev>
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

import gay.floof.gradle.utils.*
import java.text.SimpleDateFormat
import java.util.Date

buildscript {
    repositories {
        maven("https://maven.floofy.dev/repo/releases")
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
    }

    dependencies {
        classpath("org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.17.2")
        classpath("com.diffplug.spotless:spotless-plugin-gradle:6.4.2")
        classpath(kotlin("gradle-plugin", version = "1.6.20"))
        classpath(kotlin("serialization", version = "1.6.20"))
        classpath("gay.floof.utils:gradle-utils:1.3.0")
        classpath("io.kotest:kotest-gradle-plugin:0.3.9")
    }
}

plugins {
    kotlin("plugin.serialization") version "1.6.20"
    id("com.diffplug.spotless") version "6.4.2"
    kotlin("jvm") version "1.6.21"
    id("io.kotest") version "0.3.9"
    application
}

// I can't add this to the plugins block due to it being "not found"
// So, this was the only solution. :(
apply(plugin = "kotlinx-atomicfu")

val JAVA_VERSION = JavaVersion.VERSION_17
val VERSION = Version(1, 0, 0, 0, ReleaseType.Snapshot)
val COMMIT_HASH by lazy {
    val cmd = "git rev-parse --short HEAD".split("\\s".toRegex())
    val proc = ProcessBuilder(cmd)
        .directory(File("."))
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()

    proc.waitFor(1, TimeUnit.MINUTES)
    proc.inputStream.bufferedReader().readText().trim()
}

println("+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+")
println("|> Kotlin: v${KotlinVersion.CURRENT}")
println("|> Gradle: v${GradleVersion.current().toString().replace("Gradle", "").trim()}") // pipe operator in kotlin when :woeme:
println("|> hazel: $VERSION ($COMMIT_HASH)")
println("|> Java: $JAVA_VERSION (JVM: ${System.getProperty("java.version", "Unknown")} [${System.getProperty("java.vendor", "Unknown")}] | JRE: ${Runtime.version()})")
println("+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+")

group = "dev.floofy"
version = "$VERSION"

repositories {
    noelware(snapshots = true)
    mavenCentral()
    mavenLocal()
    noel()
}

dependencies {
    // Kotlin libraries
    implementation(kotlin("stdlib"))
    implementation(kotlin("reflect"))

    // BOMs
    api(platform("org.jetbrains.kotlinx:kotlinx-serialization-bom:1.3.2"))
    api(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.6.1"))
    testImplementation(platform("io.kotest:kotest-bom:5.2.3"))
    api(platform("io.ktor:ktor-bom:2.0.0"))

    // kotlinx.coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

    // kotlinx.serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
    api("org.jetbrains.kotlinx:kotlinx-serialization-core")

    // kotlinx.datetime libraries
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.2")

    // Noel Utilities
    floof("commons", "commons-slf4j", "1.3.0")
    floofy("ktor", "ktor-sentry", "0.0.1")

    // Apache Utilities
    implementation("org.apache.commons:commons-lang3:3.12.0")

    // Ktor Server
    implementation("io.ktor:ktor-server-netty")

    // Koin
    implementation("io.insert-koin:koin-core:3.1.6")

    // Logging with logback
    implementation("net.logstash.logback:logstash-logback-encoder:7.1.1")
    implementation("ch.qos.logback:logback-classic:1.2.11")
    implementation("ch.qos.logback:logback-core:1.2.11")
    api("org.slf4j:slf4j-api:1.7.36")

    // Conditional logic for logback
    implementation("org.codehaus.janino:janino:3.1.7")

    // Sentry
    implementation("io.sentry:sentry:5.7.3")
    implementation("io.sentry:sentry-logback:5.7.3")
    implementation("io.sentry:sentry-kotlin-extensions:5.7.3")

    // Prometheus (for metrics)
    implementation("io.prometheus:simpleclient_hotspot:0.15.0")
    implementation("io.prometheus:simpleclient_common:0.15.0")
    implementation("io.prometheus:simpleclient:0.15.0")

    // Remi
    implementation("org.noelware.remi:remi-support-s3:0.0.4-snapshot")
    implementation("org.noelware.remi:remi-support-fs:0.0.4-snapshot")
    api("org.noelware.remi:remi-core:0.0.4-snapshot")

    // TOML
    implementation("com.akuleshov7:ktoml-core:0.2.11")
    implementation("com.akuleshov7:ktoml-file:0.2.11")

    // Argon2
    implementation("de.mkammerer:argon2-jvm:2.11")

    // Testing utilities
    testImplementation("io.kotest:kotest-runner-junit5")
    testImplementation("io.kotest:kotest-assertions-core")
    testImplementation("io.kotest:kotest-property")
}

spotless {
    kotlin {
        trimTrailingWhitespace()
        licenseHeaderFile("${rootProject.projectDir}/assets/HEADING")
        endWithNewline()

        // We can't use the .editorconfig file, so we'll have to specify it here
        // issue: https://github.com/diffplug/spotless/issues/142
        // ktlint 0.35.0 (default for Spotless) doesn't support trailing commas
        ktlint("0.43.0")
            .userData(
                mapOf(
                    "no-consecutive-blank-lines" to "true",
                    "no-unit-return" to "true",
                    "disabled_rules" to "no-wildcard-imports,colon-spacing",
                    "indent_size" to "4"
                )
            )
    }
}

application {
    mainClass.set("dev.floofy.hazel.Bootstrap")
}

java {
    sourceCompatibility = JAVA_VERSION
    targetCompatibility = JAVA_VERSION
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = JAVA_VERSION.toString()
        kotlinOptions.javaParameters = true
        kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    }

    processResources {
        filesMatching("build-info.json") {
            val date = Date()
            val formatter = SimpleDateFormat("EEE, MMM d, YYYY - HH:mm:ss a")

            expand(
                mapOf(
                    "version" to "$VERSION",
                    "commit_sha" to COMMIT_HASH,
                    "build_date" to formatter.format(date)
                )
            )
        }
    }
}

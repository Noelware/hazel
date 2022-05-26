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

import dev.floofy.hazel.gradle.*
import dev.floofy.utils.gradle.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("plugin.serialization")
    id("com.diffplug.spotless")
    id("io.kotest")
    kotlin("jvm")
}

group = "dev.floofy.hazel"
version = "$VERSION"

println("+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+")
println("|> Kotlin: v${KotlinVersion.CURRENT}")
println("|> Gradle: v${GradleVersion.current().toString().replace("Gradle", "").trim()}") // pipe operator in kotlin when :woeme:
println("|> hazel: $VERSION ($COMMIT_HASH)")
println("|> Java: $JAVA_VERSION (JVM: ${System.getProperty("java.version", "Unknown")} [${System.getProperty("java.vendor", "Unknown")}] | JRE: ${Runtime.version()})")
println("+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+")

repositories {
    mavenCentral()
    mavenLocal()
    noelware()
    noel()
}

dependencies {
    // Kotlin libraries
    implementation(kotlin("reflect", "1.6.21"))
    implementation(kotlin("stdlib", "1.6.21"))

    // BOMs
    api(platform("org.jetbrains.kotlinx:kotlinx-serialization-bom:1.3.3"))
    api(platform("org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.6.1"))
    api(platform("org.noelware.ktor:ktor-routing-bom:0.1-beta"))
    testImplementation(platform("io.kotest:kotest-bom:5.3.0"))
    api(platform("org.noelware.remi:remi-bom:0.1.4-beta.3"))
    api(platform("dev.floofy.commons:commons-bom:2.1.0.1"))
    api(platform("io.ktor:ktor-bom:2.0.1"))

    // kotlinx.coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

    // kotlinx.serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
    api("org.jetbrains.kotlinx:kotlinx-serialization-core")

    // kotlinx.datetime libraries
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.3")

    // Noel Utilities
    implementation("dev.floofy.commons:extensions-kotlin")
    implementation("dev.floofy.commons:extensions-koin")
    implementation("dev.floofy.commons:slf4j")

    // Apache Utilities
    implementation("org.apache.commons:commons-lang3:3.12.0")

    // SLF4J
    api("org.slf4j:slf4j-api:1.7.36")
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

    kotlinGradle {
        trimTrailingWhitespace()
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

java {
    sourceCompatibility = JAVA_VERSION
    targetCompatibility = JAVA_VERSION
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = JAVA_VERSION.toString()
    kotlinOptions.javaParameters = true
    kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
}

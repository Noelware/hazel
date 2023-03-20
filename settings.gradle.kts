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


rootProject.name = "hazel"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        mavenLocal()
    }
}

plugins {
    id("com.gradle.enterprise") version "3.12.5"
}

include(
    ":cli",
    ":common",
    ":distribution:deb",
    ":distribution:rpm",
    ":modules:authentication:keystore",
    ":modules:authentication",
    ":modules:config:yaml",
    ":modules:config:dsl",
    ":modules:config",
    ":modules:logging",
    ":modules:metrics",
    ":modules:storage",
    ":plugins",
    ":plugins:loader",
    ":plugins:registry",
    ":plugins:webp-support",
    ":server"
)

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    versionCatalogs {
        create("libs") {
            from(files("./gradle/build.versions.toml"))
        }
    }
}

gradle.settingsEvaluated {
    logger.info("Checking if we can overwrite cache...")
    val overrideBuildCacheProp: String? = System.getProperty("org.noelware.gradle.overwriteCache")
    val buildCacheDir = when (val prop = System.getProperty("org.noelware.gradle.cachedir")) {
        null -> "${System.getProperty("user.dir")}/.caches/gradle"
        else -> when {
            prop.startsWith("~/") -> "${System.getProperty("user.home")}${prop.substring(1)}"
            prop.startsWith("./") -> "${System.getProperty("user.dir")}${prop.substring(1)}"
            else -> prop
        }
    }

    if (overrideBuildCacheProp == null) {
        logger.info("""
        |If you wish to override the build cache for this Gradle process, you can use the
        |-Dorg.noelware.gradle.gradle.overwriteCache=<bool> Java property in `~/.gradle/gradle.properties`
        |to overwrite it in $buildCacheDir!
        """.trimMargin("|"))
    } else {
        logger.info("Setting up build cache in directory [$buildCacheDir]")
        val file = File(buildCacheDir)
        if (!file.exists()) file.mkdirs()

        buildCache {
            local {
                directory = "$file"
                removeUnusedEntriesAfterDays = 7
            }
        }
    }

    val disableJavaSanityCheck = when {
        System.getProperty("org.noelware.gradle.ignoreJavaCheck", "false").matches("^(yes|true|1|si|si*)$".toRegex()) -> true
        (System.getenv("DISABLE_JAVA_SANITY_CHECK") ?: "false").matches("^(yes|true|1|si|si*)$".toRegex()) -> true
        else -> false
    }

    if (disableJavaSanityCheck)
        return@settingsEvaluated

    val version = JavaVersion.current()
    if (version.majorVersion.toInt() < 17)
        throw GradleException("Developing charted-server requires JDK 17 or higher, it is currently set in [${System.getProperty("java.home")}, ${System.getProperty("java.version")}] - You can ignore this check by providing the `-Dorg.noelware.gradle.ignoreJavaCheck=true` system property.")
}

val buildScanServer = System.getProperty("org.noelware.gradle.build-scan-server", "") ?: ""
gradleEnterprise {
    buildScan {
        if (buildScanServer.isNotEmpty()) {
            server = buildScanServer
            isCaptureTaskInputFiles = true
            publishAlways()
        } else {
            termsOfServiceUrl = "https://gradle.com/terms-of-service"
            termsOfServiceAgree = "yes"

            // Always publish if we're on CI.
            if (System.getenv("CI") != null) {
                publishAlways()
            }
        }

        obfuscation {
            ipAddresses { listOf("0.0.0.0") }
            hostname { "[redacted]" }
            username { "[redacted]" }
        }
    }
}

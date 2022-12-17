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
import java.text.SimpleDateFormat
import java.util.Date

plugins {
    `hazel-module`
    application
}

dependencies {
    implementation(project(":modules:authentication:keystore"))
    implementation(project(":modules:authentication"))
    implementation(libs.spring.security.crypto)
    implementation(project(":server"))
    implementation(libs.mordant)
    implementation(libs.clikt)
}

application {
    mainClass by "org.noelware.hazel.cli.CliMainKt"
}

distributions {
    main {
        distributionBaseName by "hazel"
        contents {
            into("systemd") {
                from("$projectDir/distribution/hazel.service")
            }

            into("bin") {
                from("$projectDir/distribution/bin/hazel.ps1")
                from("$projectDir/distribution/bin/hazel")
            }

            into("config") {
                from("$projectDir/distribution/config/logback.properties")
                from("$projectDir/distribution/config/hazel.yaml")
            }

            from(
                "$projectDir/distribution/README.txt",
                "$projectDir/distribution/LICENSE"
            )
        }
    }
}

tasks {
    processResources {
        filesMatching("build-info.json") {
            val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX")
            expand(
                mapOf(
                    "version" to "$VERSION",
                    "commit_sha" to VERSION.getGitCommit()!!.trim(),
                    "build_date" to formatter.format(Date()),
                    "is_nightly_build" to if (VERSION.release.suffix != "none") "true" else "false"
                )
            )
        }
    }

    distZip {
        archiveFileName by "hazel.zip"
    }

    distTar {
        archiveFileName by "hazel.tar.gz"
        compression = Compression.GZIP // use gzip for the compression :>
    }

    startScripts {
        enabled = false
    }
}

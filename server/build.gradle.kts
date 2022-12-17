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

plugins {
    `hazel-module`
}

dependencies {
    // kotlinx.coroutines debug
    implementation(libs.kotlinx.coroutines.debug)

    // Ktor Routing
    implementation(libs.noelware.ktor.routing.loaders.koin)
    implementation(libs.noelware.ktor.routing.core)


    // Spring Security Crypto
    implementation(libs.spring.security.crypto)

    // Ktor (Server)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.auto.head.response)
    implementation(libs.ktor.server.default.headers)
    implementation(libs.ktor.server.double.receive)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.serialization)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.cors)

    // Just for Log4j/JCL -> slf4j
    implementation(libs.slf4j.over.log4j)
    implementation(libs.slf4j.over.jcl)

    // Janino (for logback)
    implementation(libs.janino)

    // Modules
    implementation(project(":modules:authentication:keystore"))
    implementation(project(":modules:authentication"))
    implementation(project(":modules:config:yaml"))
    implementation(project(":modules:metrics"))
    implementation(project(":modules:storage"))
    implementation(project(":modules:logging"))
    implementation(project(":modules:config"))

    // Kaml
    implementation(libs.kaml)

    // Koin
    implementation(libs.koin)
}
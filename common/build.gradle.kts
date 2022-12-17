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
    // kotlinx.serialization
    api(libs.kotlinx.serialization.core)
    api(libs.kotlinx.serialization.json)

    // kotlinx.coroutines
    api(libs.kotlinx.coroutines.debug)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.jdk8)

    // kotlinx.datetime
    api(libs.kotlinx.datetime)

    // Logging (slf4j)
    api(libs.slf4j.api)

    // Logging (slf4j)
    api(libs.slf4j.api)

    // Noel's Utilities
    api(libs.noel.commons.extensions.kotlin)
    api(libs.noel.commons.extensions.koin)
    api(libs.noel.commons.java.utils)
    api(libs.noel.commons.slf4j)

    // Apache Utilities
    api(libs.apache.commons.lang3)

    // Sentry
    implementation(libs.sentry.kotlin.extensions)
    api(libs.sentry)

    // Remi
    api(libs.remi.storage.azure)
    api(libs.remi.storage.gcs)
    api(libs.remi.storage.s3)
    api(libs.remi.storage.fs)
    api(libs.remi.core)
}

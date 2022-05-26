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

plugins {
    `hazel-module`
}

dependencies {
    // Apache Utilities
    implementation("org.apache.commons:commons-lang3:3.12.0")

    // Ktor Server
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-server-auto-head-response")
    implementation("io.ktor:ktor-server-default-headers")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("io.ktor:ktor-server-mustache")
    implementation("io.ktor:ktor-serialization")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-cors")

    // Koin
    implementation("io.insert-koin:koin-core:3.2.0")

    // Logging with logback
    implementation("net.logstash.logback:logstash-logback-encoder:7.1.1")
    implementation("ch.qos.logback:logback-classic:1.2.11")
    implementation("ch.qos.logback:logback-core:1.2.11")
    api("org.slf4j:slf4j-api:1.7.36")

    // Conditional logic for logback
    implementation("org.codehaus.janino:janino:3.1.7")

    // Sentry
    implementation("io.sentry:sentry:5.7.4")
    implementation("io.sentry:sentry-logback:5.7.4")
    implementation("io.sentry:sentry-kotlin-extensions:5.7.4")

    // Prometheus (for metrics)
    implementation("io.prometheus:simpleclient_hotspot:0.15.0")
    implementation("io.prometheus:simpleclient_common:0.15.0")
    implementation("io.prometheus:simpleclient:0.15.0")

    // Remi
    implementation("org.noelware.remi:remi-support-minio")
    implementation("org.noelware.remi:remi-support-s3")
    implementation("org.noelware.remi:remi-support-fs")
    api("org.noelware.remi:remi-core")

    // Ktor Routing
    implementation("org.noelware.ktor:core")
    implementation("org.noelware.ktor:loader-koin")

    // TOML
    implementation("com.akuleshov7:ktoml-core:0.2.11")
    implementation("com.akuleshov7:ktoml-file:0.2.11")

    // Spring Security (argon2 hashing)
    implementation("org.springframework.security:spring-security-crypto:5.6.3")
}

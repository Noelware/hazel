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

package org.noelware.hazel.configuration.kotlin.dsl

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.noelware.hazel.configuration.kotlin.dsl.auth.HazelAuthenticationStrategy
import org.noelware.hazel.configuration.kotlin.dsl.storage.FilesystemStorageConfig
import org.noelware.hazel.configuration.kotlin.dsl.storage.StorageConfig

@Serializable
data class Config(
    /**
     * sentry dsn to use sentry for error handling
     */
    @SerialName("sentry_dsn")
    val sentryDsn: String? = null,

    /**
     * whether the frontend dashboard should be enabled
     */
    val frontend: Boolean = false,

    /**
     * whether prometheus metrics is enabled
     */
    val metrics: Boolean = true,

    /**
     * authentication strategy to use, by default, it will use none and creating/deleting artifacts will not
     * be possible.
     */
    val authentication: HazelAuthenticationStrategy = HazelAuthenticationStrategy.Disabled,

    /**
     * storage driver configuration
     */
    val storage: StorageConfig = StorageConfig(filesystem = FilesystemStorageConfig("/var/lib/noelware/hazel/data")),

    /**
     * ktor server configuration
     */
    val server: KtorServerConfig = KtorServerConfig()
)

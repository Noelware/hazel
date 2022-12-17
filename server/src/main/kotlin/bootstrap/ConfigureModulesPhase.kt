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

package org.noelware.hazel.server.bootstrap

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import dev.floofy.utils.slf4j.logging
import io.sentry.Sentry
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.EmptySerializersModule
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.noelware.hazel.HazelInfo
import org.noelware.hazel.configuration.host.ConfigurationHost
import org.noelware.hazel.configuration.host.yaml.YamlConfigurationHost
import org.noelware.hazel.configuration.kotlin.dsl.auth.HazelAuthenticationStrategy
import org.noelware.hazel.modules.authentication.keystore.HazelKeystoreAuthentication
import org.noelware.hazel.modules.metrics.PrometheusMetricsModule
import org.noelware.hazel.modules.storage.DefaultStorageDriver
import org.noelware.hazel.modules.storage.StorageDriver
import org.noelware.hazel.server.HazelServer
import org.noelware.hazel.server.endpoints.endpointsModule
import org.noelware.hazel.server.internal.DefaultHazelServer
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import java.io.File

object ConfigureModulesPhase: BootstrapPhase() {
    private val log by logging<ConfigureModulesPhase>()

    override fun bootstrap(configPath: File) {
        log.info("Loading configuration in path [$configPath]")
        val yaml = Yaml(
            EmptySerializersModule(),
            YamlConfiguration(
                encodeDefaults = true,
                strictMode = true
            )
        )

        val configHost: ConfigurationHost = if (listOf("yaml", "yml").contains(configPath.extension)) {
            YamlConfigurationHost(yaml)
        } else {
            throw IllegalStateException("Unable to determine which configuration host to use")
        }

        val config = configHost.load(configPath)
        if (config.sentryDsn != null) {
            log.info("Enabling Sentry due to [config.sentryDsn] was set")
            Sentry.init {
                it.release = "charted-server v${HazelInfo.version}+${HazelInfo.commitHash}"
                it.dsn = config.sentryDsn
            }

            log.info("Sentry is now enabled!")
        }

        val argon2 = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()
        val storage = DefaultStorageDriver(config.storage)
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            isLenient = true
        }

        val authentication = when (config.authentication) {
            is HazelAuthenticationStrategy.Keystore -> HazelKeystoreAuthentication(config.authentication as HazelAuthenticationStrategy.Keystore, argon2, json)
            else -> null
        }

        val server = DefaultHazelServer(config)
        val koinModule = module {
            single<StorageDriver> { storage }
            single<HazelServer> { server }
            single { argon2 }
            single { config }
            single { json }
        }

        val modules = mutableListOf(koinModule, endpointsModule)
        if (authentication != null) {
            modules.add(
                module {
                    single { authentication }
                }
            )
        }

        if (config.metrics) {
            val metrics = PrometheusMetricsModule()
            modules.add(
                module {
                    single { metrics }
                }
            )
        }

        startKoin {
            modules(*modules.toTypedArray())
        }
    }
}

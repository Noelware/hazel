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

package dev.floofy.hazel

import com.akuleshov7.ktoml.Toml
import com.akuleshov7.ktoml.TomlConfig
import de.mkammerer.argon2.Argon2Factory
import dev.floofy.hazel.core.KeystoreWrapper
import dev.floofy.hazel.core.StorageWrapper
import dev.floofy.hazel.data.Config
import dev.floofy.hazel.extensions.inject
import dev.floofy.hazel.routing.endpointsModule
import gay.floof.utils.slf4j.logging
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.dsl.module
import java.io.File
import java.io.IOError
import kotlin.concurrent.thread
import kotlin.system.exitProcess

object Bootstrap {
    private val log by logging<Bootstrap>()

    @JvmStatic
    fun main(args: Array<String>) {
        Thread.currentThread().name = "Hazel-BootstrapThread"
        log.info("Starting up hazel...")

        // Install the hooks
        installShutdownHook()
        installDefaultThreadExceptionHandler()

        // Configure the Hazel config
        val configPath = System.getenv("HAZEL_CONFIG_PATH") ?: "./config.toml"
        val configFile = File(configPath)

        if (!configFile.exists())
            throw IllegalArgumentException("Missing configuration path in $configPath.")

        if (configFile.extension != "toml")
            throw IllegalStateException("Configuration file $configPath must be a TOML file (must be `.toml` extension, not ${configFile.extension})")

        val toml = Toml(
            TomlConfig(
                ignoreUnknownNames = true,
                allowEmptyToml = false,
                allowEmptyValues = false,
                allowEscapedQuotesInLiteralStrings = true
            )
        )

        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            allowSpecialFloatingPointValues = true
        }

        val config = toml.decodeFromString(Config.serializer(), configFile.readText())
        val argon2 = Argon2Factory.create()
        val keystore = KeystoreWrapper(config.keystore, argon2)
        val storage = StorageWrapper(config.storage)

        keystore.init()

        // Register Koin here
        val koin = startKoin {
            modules(
                globalModule,
                endpointsModule,
                module {
                    single { argon2 }
                    single { toml }
                    single { json }
                    single { config }
                    single { keystore }
                    single { storage }
                }
            )
        }

        runBlocking {
            val hazel = koin.koin.get<Hazel>()
            try {
                hazel.start()
            } catch (e: Exception) {
                log.error("Unable to bootstrap Hazel:", e)
                exitProcess(1)
            }
        }
    }

    private fun halt(code: Int) {
        Runtime.getRuntime().halt(code)
    }

    private fun installShutdownHook() {
        val runtime = Runtime.getRuntime()
        runtime.addShutdownHook(
            thread(start = false, name = "Hazel-ShutdownThread") {
                log.warn("Shutting down Hazel...")

                // Check if Koin has started
                val koinStarted = GlobalContext.getKoinApplicationOrNull() != null
                if (koinStarted) {
                    val hazel: Hazel by inject()
                    val keystore: KeystoreWrapper by inject()

                    hazel.destroy()
                    keystore.close()
                }

                log.warn("Hazel has completely shutdown, goodbye! ｡･ﾟﾟ･(థ Д థ。)･ﾟﾟ･｡")
            }
        )
    }

    // credit: https://github.com/elastic/logstash/blob/main/logstash-core/src/main/java/org/logstash/Logstash.java#L98-L133
    private fun installDefaultThreadExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            if (e is Error) {
                log.error("Uncaught fatal error in thread ${t.name} (#${t.id}):", e)
                log.error("If this keeps occurring, please report it to Noel: https://github.com/auguwu/hazel/issues")

                var success = false

                if (e is InternalError) {
                    success = true
                    halt(128)
                }

                if (e is OutOfMemoryError) {
                    success = true
                    halt(127)
                }

                if (e is StackOverflowError) {
                    success = true
                    halt(126)
                }

                if (e is UnknownError) {
                    success = true
                    halt(125)
                }

                if (e is IOError) {
                    success = true
                    halt(124)
                }

                if (e is LinkageError) {
                    success = true
                    halt(123)
                }

                if (!success) halt(120)

                exitProcess(1)
            } else {
                log.error("Uncaught exception in thread ${t.name} (#${t.id}):", e)
            }
        }
    }
}

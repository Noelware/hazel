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

import dev.floofy.hazel.core.KeystoreWrapper
import dev.floofy.hazel.core.createThreadFactory
import dev.floofy.hazel.data.Config
import dev.floofy.hazel.extensions.formatSize
import dev.floofy.hazel.extensions.inject
import dev.floofy.hazel.extensions.retrieveAll
import dev.floofy.hazel.plugins.KtorLoggingPlugin
import dev.floofy.hazel.plugins.UserAgentPlugin
import dev.floofy.hazel.routing.AbstractEndpoint
import dev.floofy.hazel.routing.createCdnEndpoints
import gay.floof.utils.slf4j.logging
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.sentry.Sentry
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.koin.core.context.GlobalContext
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Hazel {
    companion object {
        val executorPool: ExecutorService = Executors.newCachedThreadPool(createThreadFactory("ExecutorThreadPool"))
    }

    private val routesRegistered = mutableListOf<Pair<HttpMethod, String>>()
    private lateinit var server: NettyApplicationEngine
    private val log by logging<Hazel>()

    suspend fun start() {
        val runtime = Runtime.getRuntime()
        val os = ManagementFactory.getOperatingSystemMXBean()
        val threads = ManagementFactory.getThreadMXBean()

        log.info("+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+")
        log.info("Runtime Information:")
        log.info("  * Free / Total Memory [Max]: ${runtime.freeMemory().formatSize()}/${runtime.totalMemory().formatSize()} [${runtime.maxMemory().formatSize()}]")
        log.info("  * Threads: ${threads.threadCount} (${threads.daemonThreadCount} background threads)")
        log.info("  * Operating System: ${os.name} with ${os.availableProcessors} processors (${os.arch}; ${os.version})")
        log.info("  * Versions:")
        log.info("      * JVM [JRE]: v${System.getProperty("java.version", "Unknown")} (${System.getProperty("java.vendor", "Unknown")}) [${Runtime.version()}]")
        log.info("      * Kotlin:    v${KotlinVersion.CURRENT}")
        log.info("      * Hazel:     v${HazelInfo.version} (${HazelInfo.commitHash} -- ${HazelInfo.buildDate})")

        if (HazelInfo.dediNode != null)
            log.info("  * Dedicated Node: ${HazelInfo.dediNode}")

        log.info("+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+")

        val config: Config by inject()
        val self = this

        val environment = applicationEngineEnvironment {
            developmentMode = false
            log = LoggerFactory.getLogger("dev.floofy.hazel.ktor.KtorApplicationEnvironment")

            connector {
                host = config.server.host
                port = config.server.port.toInt()
            }

            module {
                val json: Json by inject()
                val keystore: KeystoreWrapper by inject()

                install(AutoHeadResponse)
                install(KtorLoggingPlugin)
                install(UserAgentPlugin)
                install(ContentNegotiation) {
                    this.json(json)
                }

                install(Authentication) {
                    basic("hazel") {
                        realm = "Noel/Hazel"
                        validate { creds ->
                            val user = keystore.checkIfValid(creds.name, creds.password)
                            if (user) UserIdPrincipal(creds.name) else null
                        }
                    }
                }

                install(CORS) {
                    anyHost()
                    headers += "X-Forwarded-Proto"
                }

                install(DefaultHeaders) {
                    header("X-Powered-By", "Noel/Hazel (+https://github.com/auguwu/hazel; v${HazelInfo.version})")
                    header("Cache-Control", "public, max-age=7776000")

                    if (config.server.securityHeaders) {
                        header("X-Frame-Options", "deny")
                        header("X-Content-Type-Options", "nosniff")
                        header("X-XSS-Protection", "1; mode=block")
                    }

                    for ((key, value) in config.server.extraHeaders) {
                        header(key, value)
                    }
                }

                install(StatusPages) {
                    // If the route was not found :(
                    status(HttpStatusCode.NotFound) { call, _ ->
                        call.respond(
                            HttpStatusCode.NotFound,
                            buildJsonObject {
                                put("success", false)
                                put(
                                    "errors",
                                    buildJsonArray {
                                        add(
                                            buildJsonObject {
                                                put("message", "Route ${call.request.httpMethod.value} ${call.request.uri} was not found.")
                                                put("code", "UNKNOWN_ROUTE")
                                            }
                                        )
                                    }
                                )
                            }
                        )
                    }

                    // If the route has a different method handler
                    status(HttpStatusCode.MethodNotAllowed) { call, _ ->
                        call.respond(
                            HttpStatusCode.NotFound,
                            buildJsonObject {
                                put("success", false)
                                put(
                                    "errors",
                                    buildJsonArray {
                                        add(
                                            buildJsonObject {
                                                put("message", "Route ${call.request.httpMethod.value} ${call.request.uri} doesn't have a method handler.")
                                                put("code", "UNKNOWN_ROUTE")
                                            }
                                        )
                                    }
                                )
                            }
                        )
                    }

                    // General exception that we can swallow
                    exception<Exception> { call, cause ->
                        if (Sentry.isEnabled()) {
                            Sentry.captureException(cause)
                        }

                        self.log.error("Unable to handle request ${call.request.httpMethod.value} ${call.request.uri}:", cause)
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            buildJsonObject {
                                put("success", false)
                                put(
                                    "errors",
                                    buildJsonArray {
                                        add(
                                            buildJsonObject {
                                                put("message", "Unknown exception has occurred")
                                                put("code", "INTERNAL_SERVER_ERROR")
                                            }
                                        )
                                    }
                                )
                            }
                        )
                    }
                }

                routing {
                    self.log.debug("Registering file routes...")
                    val s = this
                    runBlocking {
                        s.createCdnEndpoints()
                    }

                    val endpoints = GlobalContext.retrieveAll<AbstractEndpoint>()
                    self.log.info("Found ${endpoints.size} to register!")

                    for (endpoint in endpoints) {
                        self.log.debug("${endpoint.path} [${endpoint.methods.joinToString(", ") { it.value }}]")
                        for (method in endpoint.methods) {
                            if (self.routesRegistered.contains(Pair(method, endpoint.path))) {
                                self.log.debug("Endpoint ${method.value} ${endpoint.path} is already registered.")
                                continue
                            }

                            self.routesRegistered.add(Pair(method, endpoint.path))
//                            if (endpoint.needsAuth) {
//                                authenticate("hazel") {
//                                    route(endpoint.path, method) {
//                                        handle {
//                                            endpoint.call(call)
//                                        }
//                                    }
//                                }
//                            } else {
                            route(endpoint.path, method) {
                                handle {
                                    endpoint.call(call)
                                }
                            }
//                            }
                        }
                    }
                }
            }
        }

        server = embeddedServer(Netty, environment, configure = {
            requestQueueLimit = config.server.requestQueueLimit.toInt()
            runningLimit = config.server.runningLimit.toInt()
            shareWorkGroup = config.server.shareWorkGroup
            responseWriteTimeoutSeconds = config.server.responseWriteTimeoutSeconds.toInt()
            requestReadTimeoutSeconds = config.server.requestReadTimeout.toInt()
            tcpKeepAlive = config.server.tcpKeepAlive
        })

        if (!config.server.securityHeaders)
            log.warn("It is not recommended disabling security headers~")

        server.start(wait = true)
    }

    fun destroy() {
        if (!::server.isInitialized) return

        log.warn("Destroying API server...")
        server.stop(1000, 5000)
    }
}

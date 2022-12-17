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

package org.noelware.hazel.server.internal

import dev.floofy.utils.java.SetOnce
import dev.floofy.utils.koin.retrieve
import dev.floofy.utils.slf4j.logging
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.autohead.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.netty.util.Version
import io.sentry.Sentry
import kotlinx.atomicfu.atomic
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.koin.core.context.GlobalContext
import org.noelware.hazel.HazelInfo
import org.noelware.hazel.configuration.kotlin.dsl.Config
import org.noelware.hazel.data.ApiResponse
import org.noelware.hazel.extensions.ifSentryEnabled
import org.noelware.hazel.server.HazelServer
import org.noelware.hazel.server.plugins.Logging
import org.noelware.ktor.NoelKtorRouting
import org.noelware.ktor.loader.koin.KoinEndpointLoader
import org.slf4j.LoggerFactory

internal val hasStarted = atomic(false)
internal val bootTime = System.nanoTime()

class DefaultHazelServer(private val config: Config): HazelServer {
    private val _server: SetOnce<NettyApplicationEngine> = SetOnce()
    private val log by logging<DefaultHazelServer>()

    override val started: Boolean
        get() = hasStarted.value

    override val server: ApplicationEngine
        get() = _server.value

    override fun Application.module() {
        val self = this@DefaultHazelServer

        install(AutoHeadResponse)
        install(Logging)
        install(ContentNegotiation) {
            json(GlobalContext.retrieve())
        }

        install(DefaultHeaders) {
            header("X-Powered-By", "Noelware/Hazel (+https://github.com/Noelware/hazel; v${HazelInfo.version})")
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

        // Adds error handling for status codes and exceptions that are
        // the most frequent.
        install(StatusPages) {
            // We have to do this to guard the content length since it can be null! If it is,
            // display a generic 404 message.
            statuses[HttpStatusCode.NotFound] = { call, content, _ ->
                if (content.contentLength == null) {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse.err(
                            "REST_HANDLER_NOT_FOUND", "Route handler was not found",
                            buildJsonObject {
                                put("method", call.request.httpMethod.value)
                                put("url", call.request.path())
                            }
                        )
                    )
                }
            }

            status(HttpStatusCode.MethodNotAllowed) { call, _ ->
                call.respond(
                    HttpStatusCode.MethodNotAllowed,
                    ApiResponse.err(
                        "INVALID_REST_HANDLER", "Route handler was not the right method",
                        buildJsonObject {
                            put("method", call.request.httpMethod.value)
                            put("url", call.request.path())
                        }
                    )
                )
            }

            status(HttpStatusCode.UnsupportedMediaType) { call, _ ->
                val header = call.request.header("Content-Type")
                call.respond(
                    HttpStatusCode.UnsupportedMediaType,
                    ApiResponse.err("UNSUPPORTED_CONTENT_TYPE", "Invalid content type [$header]")
                )
            }

            status(HttpStatusCode.NotImplemented) { call, _ ->
                call.respond(
                    HttpStatusCode.NotImplemented,
                    ApiResponse.err(
                        "REST_HANDLER_UNAVAILABLE", "Route handler is not implemented at this moment!",
                        buildJsonObject {
                            put("method", call.request.httpMethod.value)
                            put("url", call.request.path())
                        }
                    )
                )
            }

            exception<Exception> { call, cause ->
                ifSentryEnabled { Sentry.captureException(cause) }

                self.log.error("Unknown exception had occurred while handling request [${call.request.httpMethod.value} ${call.request.path()}]", cause)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse.err(
                        "INTERNAL_SERVER_ERROR", cause.message ?: "(unknown)",
                        buildJsonObject {
                            if (cause.cause != null) {
                                put(
                                    "cause",
                                    buildJsonObject {
                                        put("message", cause.cause!!.message ?: "(unknown)")
                                    }
                                )
                            }
                        }
                    )
                )
            }
        }

        routing {}
        install(NoelKtorRouting) {
            endpointLoader(KoinEndpointLoader)
        }
    }

    override fun start() {
        if (started) return

        log.info("Starting API server!")
        val self = this
        val environment = applicationEngineEnvironment {
            log = LoggerFactory.getLogger("org.noelware.hazel.server.KtorApplication")

            connector {
                host = self.config.server.host
                port = self.config.server.port
            }

            module {
                module()
            }
        }

        _server.value = embeddedServer(Netty, environment, configure = {
            requestQueueLimit = config.server.requestQueueLimit
            runningLimit = config.server.runningLimit
            shareWorkGroup = config.server.shareWorkGroup
            responseWriteTimeoutSeconds = config.server.responseWriteTimeoutSeconds
            requestReadTimeoutSeconds = config.server.requestReadTimeout
            tcpKeepAlive = config.server.tcpKeepAlive
        })

        val versions = Version.identify()
        val netty = versions[versions.keys.first()]!!
        log.info("Server is using Netty v${netty.artifactVersion()} (${netty.shortCommitHash()})")

        server.start(wait = true)
    }

    override fun close() {
        if (!started) return

        log.warn("Shutting down API server...")
        server.stop()
    }
}

package dev.floofy.hazel.server

import dev.floofy.hazel.server.config.Config
import dev.floofy.hazel.server.plugins.KtorLogging
import dev.floofy.hazel.server.plugins.RequestMdc
import dev.floofy.hazel.server.routing.createCdnEndpoints
import dev.floofy.hazel.server.util.Ticker
import dev.floofy.hazel.server.util.createThreadFactory
import dev.floofy.utils.koin.retrieve
import dev.floofy.utils.kotlin.sizeToStr
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
import io.sentry.Sentry
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.koin.core.context.GlobalContext
import org.noelware.ktor.NoelKtorRoutingPlugin
import org.noelware.ktor.loader.koin.KoinEndpointLoader
import org.slf4j.LoggerFactory
import java.lang.management.ManagementFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Hazel(private val config: Config): AutoCloseable {
    companion object {
        val executorPool: ExecutorService = Executors.newCachedThreadPool(createThreadFactory("ServerThreadPool"))
        val bootTime = System.currentTimeMillis()
    }

    private lateinit var server: NettyApplicationEngine
    private lateinit var tickerJob: Job
    private val log by logging<Hazel>()

    suspend fun start() {
        val runtime = Runtime.getRuntime()
        val os = ManagementFactory.getOperatingSystemMXBean()
        val threads = ManagementFactory.getThreadMXBean()

        log.info("+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+")
        log.info("Runtime Information:")
        log.info("  * Free / Total Memory [Max]: ${runtime.freeMemory().sizeToStr()}/${runtime.totalMemory().sizeToStr()} [${runtime.maxMemory().sizeToStr()}]")
        log.info("  * Threads: ${threads.threadCount} (${threads.daemonThreadCount} background threads)")
        log.info("  * Operating System: ${os.name} with ${os.availableProcessors} processors (${os.arch}; ${os.version})")
        log.info("  * Versions:")
        log.info("      * JVM [JRE]: v${System.getProperty("java.version", "Unknown")} (${System.getProperty("java.vendor", "Unknown")}) [${Runtime.version()}]")
        log.info("      * Kotlin:    v${KotlinVersion.CURRENT}")
        log.info("      * Hazel:     v${HazelInfo.version} (${HazelInfo.commitHash} -- ${HazelInfo.buildDate})")

        if (HazelInfo.dediNode != null)
            log.info("  * Dedicated Node: ${HazelInfo.dediNode}")

        log.info("+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+~+")

        val self = this
        tickerJob = Ticker("update cdn routing", config.invalidateRoutes.inWholeMilliseconds).launch {
            log.debug("Updating CDN routing...")

            val routing = server.application.plugin(Routing)
            routing.createCdnEndpoints()
        }

        val environment = applicationEngineEnvironment {
            developmentMode = System.getProperty("dev.floofy.hazel.debug", "false") == "true"
            log = LoggerFactory.getLogger("dev.floofy.hazel.ktor.KtorApplicationEnvironment")

            connector {
                host = self.config.server.host
                port = self.config.server.port.toInt()
            }

            module {
                install(KtorLogging)
                install(RequestMdc)
                install(AutoHeadResponse)
                install(ContentNegotiation) {
                    json(GlobalContext.retrieve())
                }

                install(DefaultHeaders) {
                    header("X-Powered-By", "Noel/hazel (+https://github.com/auguwu/hazel; v${HazelInfo.version})")
                    header("Cache-Control", "public, max-age=7776000")

                    if (self.config.server.securityHeaders) {
                        header("X-Frame-Options", "deny")
                        header("X-Content-Type-Options", "nosniff")
                        header("X-XSS-Protection", "1; mode=block")
                    }

                    for ((key, value) in self.config.server.extraHeaders) {
                        header(key, value)
                    }
                }

                install(StatusPages) {
                    // This is used if there is no content length (since hazel sets
                    // it in the outgoing content)
                    statuses[HttpStatusCode.NotFound] = { call, content, _ ->
                        if (content.contentLength == null) {
                            call.respond(
                                HttpStatusCode.NotFound,
                                buildJsonObject {
                                    put("success", false)
                                    putJsonArray("errors") {
                                        addJsonObject {
                                            put("code", "UNKNOWN_ROUTE")
                                            put("message", "Route ${call.request.httpMethod.value} ${call.request.uri} was not found.")
                                        }
                                    }
                                }
                            )
                        }
                    }

                    // If the route we're trying to reach was using a different method.
                    status(HttpStatusCode.MethodNotAllowed) { call, _ ->
                        call.respond(
                            HttpStatusCode.MethodNotAllowed,
                            buildJsonObject {
                                put("success", false)
                                putJsonArray("errors") {
                                    addJsonObject {
                                        put("code", "UNKNOWN_ROUTE")
                                        put("message", "Route ${call.request.httpMethod.value} ${call.request.uri} is not a valid method.")
                                    }
                                }
                            }
                        )
                    }

                    // Capture all exceptions, so we can just print "500 Internal Server Error"
                    val isDebug = System.getProperty("dev.floofy.hazel.debug", "false") == "true"
                    exception<Exception> { call, cause ->
                        if (Sentry.isEnabled()) {
                            Sentry.captureException(cause)
                        }

                        self.log.error("Unable to handle request ${call.request.httpMethod.value} ${call.request.uri}:", cause)
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            buildJsonObject {
                                put("success", false)
                                putJsonArray("errors") {
                                    addJsonObject {
                                        put("code", "INTERNAL_SERVER_ERROR")
                                        put("message", "Route ${call.request.httpMethod.value} ${call.request.uri} had thrown an exception.")

                                        if (isDebug) {
                                            putJsonObject("details") {
                                                put("error_message", cause.message ?: cause.localizedMessage)

                                                if (cause.cause != null)
                                                    put("caused_by", cause.cause!!.message ?: cause.cause!!.localizedMessage)
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

                routing {
                    runBlocking { createCdnEndpoints() }
                }

                install(NoelKtorRoutingPlugin) {
                    endpointLoader(KoinEndpointLoader)
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

    override fun close() {
        if (!::server.isInitialized) return

        log.warn("Destroying API server...")
        server.stop(1000, 5000)
        tickerJob.cancel()
    }
}

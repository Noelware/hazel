package dev.floofy.hazel.server.plugins

import dev.floofy.hazel.server.Hazel
import dev.floofy.utils.slf4j.logging
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.request.*
import io.ktor.util.*
import org.apache.commons.lang3.time.StopWatch
import java.util.concurrent.TimeUnit

val KtorLogging = createApplicationPlugin("KtorLogging") {
    val stopwatchKey = AttributeKey<StopWatch>("stopwatchKey")
    val log by logging("dev.floofy.hazel.plugins.KtorLoggingKt")

    environment?.monitor?.subscribe(ApplicationStarted) {
        log.info("HTTP server has started in ${System.currentTimeMillis() - Hazel.bootTime}ms~")
    }

    environment?.monitor?.subscribe(ApplicationStopped) {
        log.info("HTTP service has completely stopped.")
    }

    onCall { call ->
        call.attributes.put(stopwatchKey, StopWatch.createStarted())
    }

    on(ResponseSent) { call ->
        val method = call.request.httpMethod
        val version = call.request.httpVersion
        val endpoint = call.request.uri
        val status = call.response.status() ?: HttpStatusCode(-1, "Unknown HTTP Method")
        val stopwatch = call.attributes[stopwatchKey]
        val userAgent = call.request.userAgent()

        stopwatch.stop()
        log.info("${method.value} $version $endpoint :: ${status.value} ${status.description} [UA=$userAgent] [${stopwatch.getTime(
            TimeUnit.MILLISECONDS)}ms]")
    }
}

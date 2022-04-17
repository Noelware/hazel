package dev.floofy.hazel.plugins

import dev.floofy.hazel.Hazel
import gay.floof.utils.slf4j.logging
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import org.apache.commons.lang3.time.StopWatch
import java.util.concurrent.TimeUnit

class KtorLoggingPlugin {
    private val log by logging<KtorLoggingPlugin>()
    private val startTimePhase = PipelinePhase("StartTimePhase")
    private val logResponsePhase = PipelinePhase("LogResponsePhase")
    // private val prometheusObserver = AttributeKey<Histogram.Timer>("PrometheusObserver")
    private val startTimeKey = AttributeKey<StopWatch>("StartTimeKey")

    private fun install(pipeline: Application) {
        pipeline.environment.monitor.subscribe(ApplicationStarted) {
            log.info("Started HTTP service in ${Hazel.bootTime.getTime(TimeUnit.MILLISECONDS)}ms")
        }

        pipeline.environment.monitor.subscribe(ApplicationStopped) {
            log.warn("HTTP service has stopped completely!")
        }

        pipeline.addPhase(startTimePhase)
        pipeline.intercept(startTimePhase) {
            call.attributes.put(startTimeKey, StopWatch.createStarted())
        }

        pipeline.addPhase(logResponsePhase)
        pipeline.intercept(logResponsePhase) {
            logResponse(call)
        }
    }

    private fun logResponse(call: ApplicationCall) {
        val stopwatch = call.attributes[startTimeKey]
        val status = call.response.status() ?: HttpStatusCode.OK

        stopwatch.stop()
        log.info("${status.value} ${status.description} ==> ${call.request.httpMethod.value} ${call.request.uri} [${call.request.httpVersion} - ${stopwatch.getTime(TimeUnit.MILLISECONDS)}ms]")
    }

    companion object: BaseApplicationPlugin<Application, Unit, KtorLoggingPlugin> {
        override val key: AttributeKey<KtorLoggingPlugin> = AttributeKey("KtorLoggingPLugin")
        override fun install(pipeline: Application, configure: Unit.() -> Unit): KtorLoggingPlugin =
            KtorLoggingPlugin().apply { install(pipeline) }
    }
}

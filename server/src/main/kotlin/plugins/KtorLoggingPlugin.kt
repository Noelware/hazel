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

package org.noelware.hazel.server.plugins

import dev.floofy.utils.koin.inject
import dev.floofy.utils.koin.injectOrNull
import dev.floofy.utils.kotlin.doFormatTime
import dev.floofy.utils.slf4j.logging
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.application.hooks.*
import io.ktor.server.request.*
import io.ktor.util.*
import io.prometheus.client.Histogram
import org.apache.commons.lang3.time.StopWatch
import org.noelware.hazel.configuration.kotlin.dsl.Config
import org.noelware.hazel.extensions.doFormatTime
import org.noelware.hazel.modules.metrics.PrometheusMetricsModule
import org.noelware.hazel.server.internal.bootTime
import org.noelware.hazel.server.internal.hasStarted

val Logging = createApplicationPlugin("ChartedKtorLogging") {
    val stopwatchKey = AttributeKey<StopWatch>("StopWatch")
    val histogramKey = AttributeKey<Histogram.Timer>("Histogram")

    val prometheus: PrometheusMetricsModule? by injectOrNull()
    val config: Config by inject()
    val log by logging("org.noelware.charted.server.plugins.LogPluginKt")

    environment?.monitor?.subscribe(ApplicationStarted) {
        val time = (System.nanoTime() - bootTime).doFormatTime()

        log.info("API server has started [$time]")
        hasStarted.value = true
    }

    environment?.monitor?.subscribe(ApplicationStopped) {
        log.warn("API server has been stopped!")
        hasStarted.value = false
    }

    onCall { call ->
        call.attributes.put(stopwatchKey, StopWatch.createStarted())
        if (config.metrics && prometheus != null) {
            prometheus!!.ktorRequests.labels(call.request.httpMethod.value, call.request.path()).inc()
            call.attributes.put(histogramKey, prometheus!!.ktorRequestLatency.startTimer())
        }
    }

    on(ResponseSent) { call ->
        val method = call.request.httpMethod
        val version = call.request.httpVersion
        val endpoint = call.request.path()
        val status = call.response.status() ?: HttpStatusCode(-1, "Unknown HTTP Method")
        val histogram = call.attributes.getOrNull(histogramKey)
        val stopwatch = call.attributes[stopwatchKey]
        val userAgent = call.request.userAgent()

        // only applicable with tests (idk why?)
        if (stopwatch.isStarted) {
            stopwatch.stop()
            histogram?.observeDuration()
            log.info(
                "${method.value} $version $endpoint :: ${status.value} ${status.description} [$userAgent] [${stopwatch.doFormatTime()}]"
            )
        }
    }
}
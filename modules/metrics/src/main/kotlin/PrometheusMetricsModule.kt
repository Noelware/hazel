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

package org.noelware.hazel.modules.metrics

import io.prometheus.client.*
import io.prometheus.client.exporter.common.TextFormat
import io.prometheus.client.hotspot.DefaultExports
import java.io.Writer

class PrometheusMetricsModule {
    private val collectorRegistry: CollectorRegistry = CollectorRegistry()

    val ktorRequests: Counter = Counter.build("hazel_ktor_requests", "How many requests that were handled")
        .labelNames("method", "path")
        .register(collectorRegistry)

    val ktorRequestLatency: Histogram = Histogram.build("hazel_ktor_requests_latency", "Latency of all requests")
        .register(collectorRegistry)

    init {
        DefaultExports.register(collectorRegistry)
    }

    fun <W: Writer> writeIn(writer: W) {
        TextFormat.write004(writer, collectorRegistry.metricFamilySamples())
    }
}

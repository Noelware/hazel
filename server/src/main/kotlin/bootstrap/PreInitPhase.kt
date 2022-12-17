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

import dev.floofy.utils.koin.inject
import dev.floofy.utils.kotlin.sizeToStr
import dev.floofy.utils.slf4j.logging
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.koin.core.context.GlobalContext
import org.noelware.hazel.HazelInfo
import org.noelware.hazel.HazelScope
import org.noelware.hazel.server.HazelServer
import org.noelware.hazel.server.internal.hasStarted
import java.io.File
import java.io.IOError
import java.lang.management.ManagementFactory
import kotlin.concurrent.thread
import kotlin.system.exitProcess

object PreInitPhase: BootstrapPhase() {
    private val log by logging<PreInitPhase>()
    private val codes: Map<(Any) -> Boolean, Int> = mapOf(
        { i: Any -> i is InternalError } to 128,
        { i: Any -> i is OutOfMemoryError } to 127,
        { i: Any -> i is StackOverflowError } to 126,
        { i: Any -> i is UnknownError } to 125,
        { i: Any -> i is IOError } to 124,
        { i: Any -> i is LinkageError } to 123
    )

    // credit: https://github.com/elastic/logstash/blob/main/logstash-core/src/main/java/org/logstash/Logstash.java#L98-L133
    private fun installDefaultThreadExceptionHandler() {
        Thread.setDefaultUncaughtExceptionHandler { thread, ex ->
            if (ex is Error) {
                log.error("Uncaught fatal error had occurred in thread [${thread.name} (#${thread.id}):", ex)
                log.error("If this keeps occurring, report it to Noelware: https://github.com/Noelware/hazel/issues")

                for ((func, int) in codes) {
                    if (func(ex)) {
                        Runtime.getRuntime().halt(int)
                    }
                }

                exitProcess(1)
            } else {
                log.error("Uncaught exception occurred in thread [${thread.name} (#${thread.id}):", ex)

                val hadStarted = hasStarted.value
                if (!hadStarted && (thread.name matches "Hazel-(Shutdown|Bootstrap)Thread".toRegex())) {
                    exitProcess(128)
                }
            }
        }
    }

    private fun installShutdownHook() {
        val shutdownLog by logging("org.noelware.hazel.ShutdownThread")
        Runtime.getRuntime().addShutdownHook(
            thread(start = false, name = "Hazel-ShutdownThread") {
                shutdownLog.warn("Hazel server is shutting down...")

                val koin = GlobalContext.getKoinApplicationOrNull()
                if (koin != null) {
                    val server: HazelServer by inject()

                    runBlocking {
                        HazelScope.cancel()
                    }

                    server.close()
                    koin.close()
                } else {
                    shutdownLog.warn("Koin was not initialized, skipping step")
                }

                shutdownLog.warn("Hazel server has completely shutdown! ^-^")
            }
        )
    }

    override fun bootstrap(configPath: File) {
        installDefaultThreadExceptionHandler()
        installShutdownHook()

        val runtime = Runtime.getRuntime()
        val os = ManagementFactory.getOperatingSystemMXBean()

        log.info("==> Memory: total=${runtime.totalMemory().sizeToStr()} free=${runtime.freeMemory().sizeToStr()}")
        log.info("==> Kotlin: ${KotlinVersion.CURRENT}")
        log.info("==> JVM:    version=${System.getProperty("java.version")} vendor=${System.getProperty("java.vendor")}")
        log.info("==> OS:     ${os.name.lowercase()}/${os.arch} with ${os.availableProcessors} processors")

        if (HazelInfo.dedicatedNode != null) {
            log.info("==> Dedicated Node: ${HazelInfo.dedicatedNode}")
        }

        log.info("===> JVM Arguments: [${ManagementFactory.getRuntimeMXBean().inputArguments.joinToString(" ")}]")
        for (pool in ManagementFactory.getMemoryPoolMXBeans())
            log.info("===> Memory Pool [${pool.name} <${pool.type}>] ~> ${pool.peakUsage}")
    }
}

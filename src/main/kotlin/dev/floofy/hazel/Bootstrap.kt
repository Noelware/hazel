package dev.floofy.hazel

import gay.floof.utils.slf4j.logging
import org.koin.core.context.GlobalContext
import java.io.IOError
import kotlin.concurrent.thread
import kotlin.system.exitProcess

object Bootstrap {
    private val log by logging<Bootstrap>()

    @JvmStatic
    fun main(args: Array<String>) {
        Thread.currentThread().name = "Hazel-BootstrapThread"
        log.info("Starting up hazel...")
    }

    private fun halt(code: Int) {
        Runtime.getRuntime().halt(code)
    }

    private fun installShutdownHook() {
        val runtime = Runtime.getRuntime()
        runtime.addShutdownHook(thread(start = false, name = "Hazel-ShutdownThread") {
            log.warn("Shutting down Hazel...")

            // Check if Koin has started
            val koinStarted = GlobalContext.getKoinApplicationOrNull() != null
            if (koinStarted) {
                // TODO: this
            }

            log.warn("Hazel has completely shutdown, goodbye! ｡･ﾟﾟ･(థ Д థ。)･ﾟﾟ･｡")
        })
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

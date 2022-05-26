package dev.floofy.hazel.server.util

import dev.floofy.hazel.server.HazelScope
import dev.floofy.utils.slf4j.*
import kotlinx.coroutines.*
import kotlin.time.Duration.Companion.seconds

class Ticker(
    private val name: String,
    private val interval: Long = 5.seconds.inWholeMilliseconds
): CoroutineScope by HazelScope {
    private val log by logging<Ticker>()

    fun launch(block: suspend () -> Unit): Job = launch(start = CoroutineStart.DEFAULT) {
        delay(interval)
        while (isActive) {
            try {
                block()
            } catch (e: Exception) {
                log.error("Unable to execute ticker with name $name:", e)
            }

            delay(interval)
        }
    }
}


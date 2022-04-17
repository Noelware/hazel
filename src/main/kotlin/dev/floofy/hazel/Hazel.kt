package dev.floofy.hazel

import dev.floofy.hazel.core.createThreadFactory
import gay.floof.utils.slf4j.logging
import io.ktor.server.netty.*
import org.apache.commons.lang3.time.StopWatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Hazel {
    companion object {
        val executorPool: ExecutorService = Executors.newCachedThreadPool(createThreadFactory("ExecutorThreadPool"))
        val bootTime = StopWatch.createStarted()
    }

    private val routesRegistered = listOf<String>()
    private lateinit var server: NettyApplicationEngine
    private val log by logging<Hazel>()

    suspend fun start() {

    }
}

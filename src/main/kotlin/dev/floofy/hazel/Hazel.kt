/*
 * 🪶 hazel: Minimal, simple, and open source content delivery network made in Kotlin
 * Copyright 2022 Noel <cutie@floofy.dev>
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

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

package dev.floofy.hazel.core

import dev.floofy.hazel.HazelScope
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

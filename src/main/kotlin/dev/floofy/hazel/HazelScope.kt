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

import io.sentry.Sentry
import io.sentry.kotlin.SentryContext
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

object HazelScope: CoroutineScope {
    override val coroutineContext: CoroutineContext = SupervisorJob() + Hazel.executorPool.asCoroutineDispatcher()
}

/**
 * Launches a new coroutine without blocking the current thread and returns a reference to the coroutine as a [Job].
 * The coroutine is cancelled when the resulting job is [cancelled][Job.cancel].
 *
 * This extension appends the [SentryContext] coroutine context if Sentry has been initialized, this will only
 * be `true` if [Sentry.init] was called and the [coroutine context][HazelScope.coroutineContext] of the Hazel coroutine
 * scope.
 *
 * Read the documentation on [CoroutineScope.launch] for more information on how this works.
 * @param start The coroutine start option, the default will be [CoroutineStart.DEFAULT].
 * @param block The coroutine core which will be invoked by the context of the provided scope.
 */
fun HazelScope.launch(start: CoroutineStart = CoroutineStart.DEFAULT, block: suspend CoroutineScope.() -> Unit): Job =
    if (Sentry.isEnabled()) launch(SentryContext() + coroutineContext, start, block) else launch(coroutineContext, start, block)

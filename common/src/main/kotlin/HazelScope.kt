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

package org.noelware.hazel

import dev.floofy.utils.kotlin.threading.createThreadFactory
import dev.floofy.utils.slf4j.logging
import io.sentry.Sentry
import io.sentry.kotlin.SentryContext
import kotlinx.coroutines.*
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

private val coroutineExceptionHandlerLog by logging("org.noelware.hazel.coroutines.CoroutineExceptionHandler")
private val corotuineExceptionHandler = CoroutineExceptionHandler { _, t -> coroutineExceptionHandlerLog.error("Received error when executing coroutine:", t) }

/**
 * Represents a [CoroutineScope] that uses the [newCachedThreadPool][Executors.newCachedThreadPool] executor
 * that will reuse old threads if needed.
 */
object HazelScope: CoroutineScope {
    override val coroutineContext: CoroutineContext = SupervisorJob() + Executors.newCachedThreadPool(
        createThreadFactory("Hazel-CoroutinesExecutor")
    ).asCoroutineDispatcher()
}

/**
 * Launches a new coroutine without blocking the current thread and returns a reference to the coroutine as a [Job].
 * The coroutine is cancelled when the resulting job is [cancelled][Job.cancel].
 *
 * By default, the coroutine is immediately scheduled for execution.
 * Other start options can be specified via `start` parameter. See [CoroutineStart] for details.
 * An optional [start] parameter can be set to [CoroutineStart.LAZY] to start coroutine _lazily_. In this case,
 * the coroutine [Job] is created in _new_ state. It can be explicitly started with [start][Job.start] function
 * and will be started implicitly on the first invocation of [join][Job.join].
 *
 * This extension is only for use when using the [HazelScope] for scheduling coroutine jobs. This attaches an
 * [SentryContext] if Sentry is enabled on the server, otherwise, it'll use the [coroutineContext][HazelScope.coroutineContext]
 * of [HazelScope].
 *
 * @param start coroutine start option. The default value is [CoroutineStart.DEFAULT].
 * @param block the coroutine code which will be invoked in the context of the provided scope.
 */
fun HazelScope.launch(
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
): Job = CoroutineScope(if (Sentry.isEnabled()) coroutineContext + SentryContext() else coroutineContext).launch(
    corotuineExceptionHandler,
    start,
    block
)

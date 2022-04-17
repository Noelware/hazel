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
fun HazelScope.launch(start: CoroutineStart = CoroutineStart.DEFAULT, block: CoroutineScope.() -> Unit): Job =
    if (Sentry.isEnabled()) launch(SentryContext() + coroutineContext, start, block) else launch(coroutineContext, start, block)

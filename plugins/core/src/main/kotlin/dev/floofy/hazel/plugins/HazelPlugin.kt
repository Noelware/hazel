@file:Suppress("UNUSED")
package dev.floofy.hazel.plugins

import dev.floofy.hazel.plugins.context.BootstrapContext
import dev.floofy.hazel.plugins.context.DestroyContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KCallable
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.hasAnnotation

/**
 * Represents the base plugin class.
 */
open class HazelPlugin {
    private val bootstrapWasCalled = AtomicBoolean(false)
    private val closeMethodWasCalled = AtomicBoolean(false)

    private val bootstrapMethod: KCallable<*>?
        get() = this::class.members.firstOrNull { it.hasAnnotation<BootstrapAction>() }

    private val closeMethod: KCallable<*>?
        get() = this::class.members.firstOrNull { it.hasAnnotation<DestroyAction>() }

    /**
     * Returns the information collected from the `plugin-info.json` file.
     */
    @OptIn(ExperimentalSerializationApi::class)
    val info: PluginInfo
        get() {
            val resource = this::class.java.getResourceAsStream("/plugin-info.json")
                ?: throw IllegalStateException("Missing `plugin-info.json` in resources/")

            return Json.decodeFromStream(resource)
        }

    /**
     * Calls the method with the [BootstrapAction] annotation attached to it.
     * @param context The context object of when the plugin is being bootstrapped.
     */
    suspend fun callBootstrap(context: BootstrapContext) {
        if (bootstrapMethod == null)
            return

        if (bootstrapWasCalled.compareAndSet(false, true)) {
            if (bootstrapMethod!!.isSuspend)
                bootstrapMethod!!.callSuspend(this, context)
            else
                bootstrapMethod!!.call(this, context)
        }
    }

    suspend fun callDestroy(context: DestroyContext) {
        if (closeMethod == null)
            return

        if (closeMethodWasCalled.compareAndSet(false, true)) {
            if (closeMethod!!.isSuspend)
                closeMethod!!.callSuspend(this, context)
            else
                closeMethod!!.call(this, context)
        }
    }
}

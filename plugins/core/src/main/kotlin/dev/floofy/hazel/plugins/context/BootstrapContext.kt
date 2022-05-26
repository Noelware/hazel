package dev.floofy.hazel.plugins.context

import dev.floofy.hazel.plugins.util.AttributeKey
import dev.floofy.hazel.plugins.util.Attributes

class BootstrapContext {
    /**
     * List of attributes to collect from.
     */
    val attributes = Attributes()
}

@Suppress("UNUSED")
fun <T> BootstrapContext.getAttribute(key: AttributeKey<T>): T = attributes[key] as? T ?: error("Unable to get attribute $key")

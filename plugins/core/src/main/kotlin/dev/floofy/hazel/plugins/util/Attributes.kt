package dev.floofy.hazel.plugins.util

class Attributes: MutableMap<AttributeKey<*>, Any> by mutableMapOf() {
    @JvmName("getCasted")
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: AttributeKey<T>): T? = getOrNull(key) ?: error("Unable to fetch key $key")

    @Suppress("UNCHECKED_CAST")
    fun <T> getOrNull(key: AttributeKey<T>): T? = if (contains(key)) this[key] as? T else null
}

package dev.floofy.hazel.plugins.util

/**
 * Represents a key that can be collected through the context's attributes.
 */
class AttributeKey<T>(private val name: String) {
    override fun toString(): String = "AttributeKey($name)"
}

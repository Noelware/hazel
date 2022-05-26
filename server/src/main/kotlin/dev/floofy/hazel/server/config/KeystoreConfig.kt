package dev.floofy.hazel.server.config

@kotlinx.serialization.Serializable
data class KeystoreConfig(
    val path: String,
    val password: String? = null
)

package dev.floofy.hazel.plugins

/**
 * Represents the information of this [plugin][HazelPlugin].
 * @param name The name of the plugin, must be 2-32 characters of length.
 * @param description The description of the plugin, must be over 1-160 chars in length.
 * @param version A valid SemVer string of the current version of this plugin
 * @param className The class name to load this plugin from, it must extend [HazelPlugin].
 */
@kotlinx.serialization.Serializable
data class PluginInfo(
    val name: String,
    val description: String = "No description has been specified.",
    val version: String = "1.0.0",
    val className: String
)

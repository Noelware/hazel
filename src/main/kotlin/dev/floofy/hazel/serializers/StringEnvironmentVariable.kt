package dev.floofy.hazel.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

val ENV_KEY_REGEX = "[\$]\\{([\\w\\.]+)\\}".toRegex()

/**
 * Represents a serializer for reading the environment variable of a given property
 * if it was encapsulated with `${...}`.
 */
object StringEnvironmentVariable: KSerializer<String> {
    override val descriptor: SerialDescriptor = String.serializer().descriptor

    override fun deserialize(decoder: Decoder): String {
        val data = decoder.decodeString()
        if (data.matches(ENV_KEY_REGEX)) {
            val matcher = ENV_KEY_REGEX.toPattern().matcher(data)
            if (!matcher.matches()) return data

            val envKey = matcher.group(1)
            return System.getenv(envKey)
        }

        return data
    }

    override fun serialize(encoder: Encoder, value: String) {
        val serializer = String.serializer()
        return serializer.serialize(encoder, value)
    }
}

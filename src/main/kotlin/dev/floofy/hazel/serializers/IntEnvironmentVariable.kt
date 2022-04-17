package dev.floofy.hazel.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object IntEnvironmentVariable: KSerializer<Int> {
    override val descriptor: SerialDescriptor = Int.serializer().descriptor

    override fun deserialize(decoder: Decoder): Int {
        val int = try {
            decoder.decodeString()
        } catch (e: Exception) {
            null
        } ?: return decoder.decodeInt()

        val matcher = ENV_KEY_REGEX.toPattern().matcher(int)
        if (!matcher.matches()) return Integer.parseInt(int)

        val envKey = matcher.group(1)
        return Integer.parseInt(System.getenv(envKey))
    }

    override fun serialize(encoder: Encoder, value: Int) = Int.serializer().serialize(encoder, value)
}

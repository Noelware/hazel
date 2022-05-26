package dev.floofy.hazel.server.serializers

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration

private val DURATION_REGEX = "(\\d+)\\s([ms|m|h|hr|hour|minute|min|s|sec|seconds|minutes|hours|milliseconds|d|days|day]+)".toRegex()

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = Duration::class)
object DurationSerializer: KSerializer<Duration> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.Duration", PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: Duration) {
        encoder.encodeLong(value.inWholeMilliseconds)
    }

    override fun deserialize(decoder: Decoder): Duration {
        // If we can decode it as a string, and it matches DURATION_REGEX, return it!
        return try {
            val res = decoder.decodeString()
            val matcher = DURATION_REGEX.toPattern().matcher(res)

            if (matcher.matches()) {
                val int = Integer.parseInt(matcher.group(1))
                when (matcher.group(2)) {
                    "ms", "millisecond", "milliseconds" -> int.milliseconds
                    "m", "minute", "min", "minutes" -> int.minutes
                    "h", "hour", "hours" -> int.hours
                    "s", "second", "seconds", "sec" -> int.seconds
                    "d", "days", "day" -> int.days
                    else -> error("Unable to parse ${matcher.group(2)} to Duration.")
                }
            }

            throw error("i know this is unreachable but kotlin please")
        } catch (e: SerializationException) {
            decoder.decodeLong().toDuration(DurationUnit.MILLISECONDS)
        }
    }
}

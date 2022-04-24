/*
 * 🪶 hazel: Minimal, simple, and open source content delivery network made in Kotlin
 * Copyright 2022 Noel <cutie@floofy.dev>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.floofy.hazel.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object IntEnvironmentVariable: KSerializer<Long> {
    override val descriptor: SerialDescriptor = Long.serializer().descriptor

    override fun deserialize(decoder: Decoder): Long {
        val int = try {
            decoder.decodeString()
        } catch (e: Exception) {
            null
        } ?: return decoder.decodeLong()

        val matcher = ENV_KEY_REGEX.toPattern().matcher(int)
        if (!matcher.matches()) return int.toLongOrNull() ?: error("Unable to cast value from a string -> long :(")

        val envKey = matcher.group(1)
        return System.getenv(envKey).toLongOrNull() ?: error("Unable to cast value from a string -> long")
    }

    override fun serialize(encoder: Encoder, value: Long) = Long.serializer().serialize(encoder, value)
}

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

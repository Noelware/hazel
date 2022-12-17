/*
 * 🪶 Hazel: Minimal, and fast HTTP proxy to host files from any cloud storage provider.
 * Copyright 2022-2023 Noelware <team@noelware.org>
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

package org.noelware.hazel.configuration.kotlin.dsl.auth

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
import kotlinx.serialization.encoding.CompositeDecoder.Companion.DECODE_DONE
import org.noelware.hazel.serializers.SecretStringSerializer

@Serializable
enum class AuthStrategyType {
    @SerialName("disabled")
    DISABLED,

    @SerialName("keystore")
    KEYSTORE,

    @SerialName("ldap")
    LDAP;
}

@Serializable(with = HazelAuthenticationStrategy.Serializer::class)
sealed class HazelAuthenticationStrategy(val authStrategy: AuthStrategyType) {
    @Suppress("UNUSED")
    constructor(): this(AuthStrategyType.DISABLED)

    @Serializable
    object Disabled: HazelAuthenticationStrategy(AuthStrategyType.DISABLED)

    @Serializable
    class Keystore(
        val path: String = "./hazel.keystore.jks",

        @Serializable(with = SecretStringSerializer::class)
        val password: String? = null
    ): HazelAuthenticationStrategy(AuthStrategyType.KEYSTORE)

    @Serializable
    object LDAP: HazelAuthenticationStrategy(AuthStrategyType.LDAP)

    internal class Serializer: KSerializer<HazelAuthenticationStrategy> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("hazel.AuthStrategy") {
            element<AuthStrategyType>("type")

            // keystore settings
            element<String>("path", isOptional = true)
            element<String>("password", isOptional = true)
        }

        override fun deserialize(decoder: Decoder): HazelAuthenticationStrategy = decoder.decodeStructure(descriptor) {
            var authStrategyType: AuthStrategyType? = null
            var strategy: HazelAuthenticationStrategy? = null

            // keystore params
            var keystorePath: String? = null

            loop@ while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    DECODE_DONE -> break
                    0 -> {
                        authStrategyType = decodeSerializableElement(descriptor, 0, AuthStrategyType.serializer())
                        if (authStrategyType == AuthStrategyType.DISABLED) {
                            strategy = Disabled
                            break
                        }
                    }

                    // 1 => path, we need to assume `authStrategyType` is KEYSTORE
                    1 -> {
                        if (authStrategyType != null && authStrategyType == AuthStrategyType.KEYSTORE) {
                            keystorePath = decodeStringElement(descriptor, index)
                        }
                    }

                    // 2 => password, we need to assume `authStrategyType` is KEYSTORE
                    2 -> {
                        if (authStrategyType != null && authStrategyType == AuthStrategyType.KEYSTORE) {
                            if (keystorePath == null) throw SerializationException("Unexpected index $index (expected: path, received: password)")
                            strategy = Keystore(keystorePath, decodeStringElement(descriptor, index))
                        }
                    }

                    else -> throw SerializationException("Unexpected index $index")
                }
            }

            assert(strategy != null)
            strategy!!
        }

        override fun serialize(encoder: Encoder, value: HazelAuthenticationStrategy) = encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, AuthStrategyType.serializer(), value.authStrategy)

            when (value) {
                // skip
                is Disabled -> {}
                is Keystore -> {
                    encodeStringElement(descriptor, 1, value.path)
                    if (value.password != null) {
                        encodeStringElement(descriptor, 2, value.password)
                    }
                }

                // skip
                is LDAP -> {}
            }
        }
    }
}

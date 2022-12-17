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

package org.noelware.hazel.cli

import com.github.ajalt.clikt.core.CliktCommand
import kotlinx.serialization.json.Json
import org.noelware.hazel.configuration.kotlin.dsl.auth.AuthStrategyType
import org.noelware.hazel.configuration.kotlin.dsl.auth.HazelAuthenticationStrategy
import org.noelware.hazel.modules.authentication.keystore.HazelKeystoreAuthentication
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder

abstract class KeystoreAwareCommand(name: String, help: String = ""): CliktCommand(help, name) {
    fun getKeystoreAuthenticator(config: HazelAuthenticationStrategy): HazelKeystoreAuthentication? {
        if (config.authStrategy != AuthStrategyType.KEYSTORE) return null
        return HazelKeystoreAuthentication(
            config as HazelAuthenticationStrategy.Keystore,
            Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8(),
            Json
        ).also { it.init() }
    }
}

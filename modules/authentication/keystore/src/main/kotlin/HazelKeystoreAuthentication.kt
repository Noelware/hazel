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

package org.noelware.hazel.modules.authentication.keystore

import dev.floofy.utils.kotlin.ifNotNull
import dev.floofy.utils.slf4j.logging
import kotlinx.atomicfu.atomic
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.noelware.hazel.configuration.kotlin.dsl.auth.HazelAuthenticationStrategy
import org.noelware.hazel.modules.authentication.AuthenticationHost
import org.noelware.hazel.modules.authentication.User
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.KeyStore.PasswordProtection
import javax.crypto.spec.SecretKeySpec

class HazelKeystoreAuthentication(
    private val config: HazelAuthenticationStrategy.Keystore,
    private val argon2: Argon2PasswordEncoder,
    private val json: Json
): AuthenticationHost, Closeable {
    private val keystore = KeyStore.getInstance(KeyStore.getDefaultType())
    private val closed = atomic(false)
    private val log by logging<HazelKeystoreAuthentication>()

    override fun init() {
        if (closed.value) return

        log.info("Initializing keystore!")
        if (config.password == null) log.warn("Recommended to set a password for your keystore!")

        val passwordArray = (config.password ?: "").toCharArray()
        val keystoreFile = File(config.path)

        if (!keystoreFile.exists()) {
            keystore.load(null, passwordArray)
        } else {
            keystore.load(keystoreFile.inputStream(), passwordArray)
        }
    }

    fun get(user: String): User? {
        if (closed.value) throw IllegalStateException("Keystore is currently closed, cannot do operation [GET_USER $user]")
        if (!keystore.containsAlias("users:$user")) return null

        val entry = keystore.getEntry("users:$user", PasswordProtection((config.password ?: "").toCharArray())) as KeyStore.SecretKeyEntry
        return json.decodeFromString(String(entry.secretKey.encoded))
    }

    fun create(user: String, password: String, roles: List<String>): User {
        if (closed.value) throw IllegalStateException("Keystore is currently closed, cannot do operation [CREATE_USER $user]")
        if (keystore.containsAlias("users:$user")) throw IllegalArgumentException("User with username $user was already created!")

        val createdUser = User(
            user,
            argon2.encode(password),
            roles
        )

        val spec = SecretKeySpec(json.encodeToString(createdUser).toByteArray(), "AES")
        keystore.setEntry("users:$user", KeyStore.SecretKeyEntry(spec), PasswordProtection((config.password ?: "").toCharArray()))
        flush()

        return createdUser
    }

    private fun flush() {
        if (closed.value) throw IllegalStateException("Keystore is currently closed, cannot do operation [FLUSH_CHANGES]")

        log.debug("Flushing changes from keystore into filesystem [${config.path}]")
        return FileOutputStream(File(config.path)).use { keystore.store(it, (config.password ?: "").toCharArray()) }
    }

    override fun doAuthenticate(username: String, password: String): Boolean {
        if (closed.value) throw IllegalStateException("Keystore is currently closed, cannot do operation [DO_AUTHENTICATE $username ${"*".repeat(password.length)}]")

        val user = get(username) ?: return false
        return argon2.matches(password, user.password)
    }

    override fun hasRole(user: String, role: String): Boolean {
        if (closed.value) throw IllegalStateException("Keystore is currently closed, cannot do operation [HAS_ROLE $user $role]")
        return get(user).ifNotNull { this.roles.contains(role) } ?: false
    }

    override fun close() {
        if (closed.value) return

        flush()
        closed.value = true
    }
}

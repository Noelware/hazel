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

package dev.floofy.hazel.core

import de.mkammerer.argon2.Argon2
import dev.floofy.hazel.data.KeystoreConfig
import gay.floof.utils.slf4j.logging
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.KeyStore.PasswordProtection
import javax.crypto.spec.SecretKeySpec

/**
 * Represents the wrapper for Java's Keystore functionality. hazel uses keystores
 * for user authentication for sensitive endpoints (i.e, `POST`/`PUT`/`DELETE`)
 */
class KeystoreWrapper(private val config: KeystoreConfig, private val argon2: Argon2): AutoCloseable {
    private val keystore = KeyStore.getInstance(KeyStore.getDefaultType())
    private val log by logging<KeystoreWrapper>()
    private var closed = false

    fun init() {
        // nop this if it's already closed; we do not re-create this instance
        if (closed) return
        if (config.password == null) {
            log.warn("It is recommended to set a password for your keystore!")
        }

        log.info("Creating users keystore...")
        val passwordArray = (config.password ?: "").toCharArray()
        keystore.load(FileInputStream(File(config.file)), passwordArray)

        log.info("Done!")
    }

    fun addUser(username: String, password: String) {
        assert(!closed) { "Keystore is currently closed, cannot do operation: ADD_USER $username -> [...]" }

        // TODO: do not actually encode the password here >:(
        val hash = argon2.hash(10, 65536, 1, password.toByteArray())
        val spec = SecretKeySpec(hash.toByteArray(), "AES")
        keystore.setEntry("users:$username", KeyStore.SecretKeyEntry(spec), PasswordProtection((config.password ?: "").toCharArray()))
        save()
    }

    fun checkIfValid(username: String, password: String): Boolean {
        assert(!closed) { "Keystore is currently closed, cannot do operation: IS_VALID $username -> [...]" }

        if (!keystore.containsAlias("users:$username")) return false

        val entry = keystore.getEntry("users:$username", PasswordProtection((config.password ?: "").toCharArray())) as KeyStore.SecretKeyEntry
        val value = String(entry.secretKey.encoded)

        return argon2.verify(value, password.toByteArray())
    }

    override fun close() {
        // If it's closed already, do nothing.
        if (closed) return

        closed = true

        save()
        log.info("Closed Keystore! We will no longer to use operations.")
    }

    private fun save() {
        assert(!closed) { "Keystore is currently closed, cannot do operation: SAVE KEYSTORE" }

        log.debug("Flushing keystore to filesystem...")
        val fos = FileOutputStream(File(config.file))
        fos.use {
            keystore.store(it, (config.password ?: "").toCharArray())
        }
    }
}

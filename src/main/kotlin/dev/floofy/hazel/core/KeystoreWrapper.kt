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
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyStore
import java.security.KeyStore.PasswordProtection
import javax.crypto.spec.SecretKeySpec

/**
 * Represents the wrapper for Java's Keystore functionality. hazel uses a keystore
 * for user authentication for sensitive endpoints (i.e, `POST`/`PUT`/`DELETE`)
 */
class KeystoreWrapper(
    private val config: KeystoreConfig,
    private val argon2: Argon2
): AutoCloseable {
    private val keystore = KeyStore.getInstance(KeyStore.getDefaultType())
    private val log by logging<KeystoreWrapper>()

    // this is only visible for testing
    var closed = false

    fun createIfNotExists() {
        // nop this if it's already closed; we do not re-create this instance
        if (closed) return
        if (config.password == null) {
            log.warn("It is recommended to set a password for your keystore!")
        }

        log.debug("Checking if keystore exists in ${config.file}...")
        val passwordArray = (config.password ?: "").toCharArray()
        val file = File(config.file)

        if (!file.exists()) {
            keystore.load(null, passwordArray)
        } else {
            throw IllegalStateException("Keystore already exists in path ${config.file}!")
        }

        log.debug("Done, now flushing to filesystem...")
        close()
    }

    fun init() {
        // nop this if it's already closed; we do not re-create this instance
        if (closed) return
        if (config.password == null) {
            log.warn("It is recommended to set a password for your keystore!")
        }

        log.debug("Loading keystore in path ${config.file}...")
        val passwordArray = (config.password ?: "").toCharArray()
        val file = File(config.file)

        if (!file.exists()) {
            keystore.load(null, passwordArray)
        } else {
            keystore.load(FileInputStream(file), passwordArray)
        }

        log.debug("Done!")
    }

    fun initUnsafe() {
        // nop this if it's already closed; we do not re-create this instance
        if (closed) return
        if (config.password == null) {
            log.warn("It is recommended to set a password for your keystore!")
        }

        log.debug("Loading keystore in path ${config.file}...")
        val passwordArray = (config.password ?: "").toCharArray()
        val file = File(config.file)

        if (!file.exists())
            throw IllegalStateException("Keystore file doesn't exist in path ${config.file}, please run `hazel keystore create` to generate a keystore!")

        keystore.load(FileInputStream(file), passwordArray)
        log.debug("Done!")
    }

    operator fun get(key: String): String {
        if (closed)
            throw IllegalStateException("Keystore is currently closed, cannot do operation: GET $key")

        if (!keystore.containsAlias(key))
            throw IllegalStateException("Alias $key doesn't exist in keystore.")

        val k = keystore.getKey(key, (config.password ?: "").toCharArray())
        return String(k.encoded)
    }

    fun list(): List<String> {
        if (closed)
            throw IllegalStateException("Keystore is currently closed, cannot do operation: LIST_KEYS")

        return keystore.aliases().toList()
    }

    fun addValue(key: String, value: String) {
        if (closed)
            throw IllegalStateException("Keystore is currently closed, cannot do operation: ADD_VALUE $key -> [...]")

        if (keystore.containsAlias(key))
            throw IllegalStateException("Key $key already exists.")

        keystore.setEntry(key, KeyStore.SecretKeyEntry(SecretKeySpec(value.toByteArray(), "AES")), PasswordProtection((config.password ?: "").toCharArray()))
        save()
    }

    fun addUser(username: String, password: String) {
        if (closed) {
            throw IllegalStateException("Keystore is currently closed, cannot do operation: ADD_USER $username -> [...]")
        }

        // Check if the alias is already there
        if (keystore.containsAlias("users:$username"))
            throw IllegalStateException("User $username already exists in keystore!")

        val hash = argon2.hash(10, 65536, 1, password.toByteArray())
        val spec = SecretKeySpec(hash.toByteArray(), "AES")
        keystore.setEntry("users:$username", KeyStore.SecretKeyEntry(spec), PasswordProtection((config.password ?: "").toCharArray()))
        save()
    }

    fun checkIfValid(username: String, password: String): Boolean {
        if (closed) {
            throw IllegalStateException("Keystore is currently closed, cannot do operation: IS_VALID $username -> [...]")
        }

        if (!keystore.containsAlias("users:$username")) return false

        val entry = keystore.getEntry("users:$username", PasswordProtection((config.password ?: "").toCharArray())) as KeyStore.SecretKeyEntry
        val value = String(entry.secretKey.encoded)

        return argon2.verify(value, password.toByteArray())
    }

    fun deleteUser(username: String) {
        if (closed)
            throw IllegalStateException("Keystore is currently closed, cannot do operation: DELETE USER $username")

        if (!keystore.containsAlias("users:$username"))
            throw IllegalStateException("User $username is non existent in keystore.")

        keystore.deleteEntry("users:$username")
    }

    fun deleteKeystore() {
        if (closed)
            throw IllegalStateException("Keystore is currently closed, cannot do operation: DELETE KEYSTORE IN ${config.file}")

        log.warn("!!! THIS IS A DESTRUCTIVE OPERATION DO NOT DO THIS UNLESS YOU'RE RE-DOING YOUR INSTALLATION !!!")
        val aliases = keystore.aliases()
        while (aliases.hasMoreElements()) {
            val alias = aliases.nextElement()
            keystore.deleteEntry(alias)
        }

        Files.deleteIfExists(Paths.get(config.file))

        closed = true
        log.warn("Keystore is now successfully deleted from disk, all future authentication will not succeed!")
    }

    override fun close() {
        // If it's closed already, do nothing.
        if (closed) return

        save() // flush to disk so we can load it back

        closed = true
        log.info("Successfully closed keystore! We will no longer be able to execute operations. :<")
    }

    private fun save() {
        if (closed) {
            throw IllegalStateException("Keystore is currently closed, cannot do operation: SAVE KEYSTORE")
        }

        log.debug("Flushing keystore to filesystem...")
        val fos = FileOutputStream(File(config.file))
        fos.use {
            keystore.store(it, (config.password ?: "").toCharArray())
        }
    }
}

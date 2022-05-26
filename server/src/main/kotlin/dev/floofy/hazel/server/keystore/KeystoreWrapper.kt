package dev.floofy.hazel.server.keystore

import dev.floofy.utils.slf4j.logging
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.KeyStore.PasswordProtection
import javax.crypto.spec.SecretKeySpec

class KeystoreWrapper(private val path: String, private val password: String? = null): AutoCloseable {
    private val keystore = KeyStore.getInstance(KeyStore.getDefaultType())
    private var closed = false
    private val log by logging<KeystoreWrapper>()

    init {
        if (password == null) {
            log.warn("It is recommended to set a keystore password!")
        }

        log.debug("Loading keystore in path $path...")
        val file = File(path)

        if (file.exists()) {
            keystore.load(FileInputStream(file), (password ?: "").toCharArray())
        } else {
            keystore.load(null, (password ?: "").toCharArray())
        }
    }

    operator fun get(key: String): String {
        if (closed)
            throw java.lang.IllegalStateException("Keystore is closed, cannot do operation: GET $key")

        if (!keystore.containsAlias(key))
            throw IllegalStateException("Alias $key doesn't exist in keystore.")

        return String(keystore.getKey(key, (password ?: "").toCharArray()).encoded)
    }

    operator fun set(key: String, value: String) {
        if (closed)
            throw java.lang.IllegalStateException("Keystore is closed, cannot do operation: SET $key ${"*".repeat(value.length)}")

        if (keystore.containsAlias(key))
            throw IllegalStateException("Key $key already exists.")

        val keySpec = SecretKeySpec(value.toByteArray(), "AES")
        val protection = PasswordProtection((password ?: "").toCharArray())

        keystore.setEntry(key, KeyStore.SecretKeyEntry(keySpec), protection)
        save()
    }

    fun addUser(username: String, password: String) {
        if (closed)
            throw java.lang.IllegalStateException("Keystore is closed, cannot do operation: ADD USER $username ${"*".repeat(password.length)}")

        if (keystore.containsAlias("users:$username"))
            throw java.lang.IllegalStateException("User with username $username already exists!")

        val pass = Argon2PasswordEncoder().encode(password)
        this["users:$username"] = pass
    }

    fun deleteUser(username: String) {
        if (closed)
            throw IllegalStateException("Keystore is currently closed, cannot do operation: DELETE USER $username")

        if (!keystore.containsAlias("users:$username"))
            throw IllegalStateException("User $username is non existent in keystore.")

        keystore.deleteEntry("users:$username")
    }

    fun checkPasswordValid(username: String, pass: String): Boolean {
        if (closed)
            throw IllegalStateException("Keystore is currently closed, cannot do operation: USER PASSWORD CORRECT FOR $username = ${"*".repeat(pass.length)}")

        if (!keystore.containsAlias("users:$username"))
            return false

        val entry = keystore.getEntry("users.$username", PasswordProtection((password ?: "").toCharArray())) as KeyStore.SecretKeyEntry
        val argon2 = Argon2PasswordEncoder()

        return argon2.matches(pass, String(entry.secretKey.encoded))
    }

    override fun close() {
        // If it's closed already, do nothing.
        if (closed) return

        // Flush to the disk, so we can load it back once it restarts.
        save()

        closed = true
        log.info("Successfully closed the keystore!")
    }

    private fun save() {
        if (closed)
            throw IllegalStateException("Keystore is currently closed, cannot do operation: SAVE KEYSTORE DATA")

        log.debug("Flushing keystore to local disk...")
        FileOutputStream(File(path)).use { keystore.store(it, (password ?: "").toCharArray()) }
    }
}

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

package dev.floofy.hazel.cli.keystore

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.mkammerer.argon2.Argon2Factory
import dev.floofy.hazel.core.KeystoreWrapper
import dev.floofy.hazel.data.KeystoreConfig
import okhttp3.internal.closeQuietly
import java.io.BufferedReader
import java.io.CharArrayWriter
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

object AddUserToKeystoreCommand: CliktCommand(
    name = "add-user",
    help = """
    |The `add-user` subcommand registers a user to execute sensitive data towards your
    |storage for the CDN. Please, only give this to trusted people! In the future, Hazel
    |will have a permissions system.
    """.trimMargin()
) {
    private val user: String by argument("user", help = "The username to use")
    private val userPassword: String? by argument("pwd", help = "The password to use. Use `--stdin` to populate this with stdin.")
        .optional()

    private val path: String? by option("--path", "--ks-path", "-p", help = "Returns the keystore path to use.").required()
    private val password: String? by option("--password", "--ks-pwd", "--ps", help = "The keystore password that was used to save the keystore.", envvar = "HAZEL_KEYSTORE_PASSWORD")
    private val stdin: Boolean? by option("--stdin", "-x", help = "If we should use the stdin to get the user's password.")
        .flag(default = false)

    override fun run() {
        val cwd = System.getProperty("user.dir", "")
        val home = System.getProperty("user.home", "/")

        val actualPath = when {
            path == null -> error("Keystore path was not defined. -w-")
            path?.startsWith("~/") == true -> home + path!!
            path?.startsWith("./") == true -> (cwd + path!!.replaceFirstChar { "" }).trim()
            else -> path!!
        }

        val keystore = KeystoreWrapper(
            KeystoreConfig(
                actualPath,
                password
            ),
            Argon2Factory.create()
        )

        // We use the initUnsafe function, so it can throw an error instead of creating
        // the keystore!
        keystore.initUnsafe()

        // Check if we need to use the stdin
        val password: String
        val shouldUseStdin = stdin ?: false
        if (shouldUseStdin) {
            val reader = BufferedReader(InputStreamReader(System.`in`, StandardCharsets.UTF_8))
            val passArray = CharArrayWriter().use {
                var done = false

                while (!done) {
                    val char = reader.read()
                    if (char.toChar() == '\r' || char.toChar() == '\n') {
                        done = true
                    }

                    it.write(char)
                }

                it
            }.toCharArray()

            password = String(passArray)

            // Close the stdin reader quietly
            reader.closeQuietly()
        } else {
            password = userPassword ?: error("Missing `pwd` value (use `--stdin`/`-x` to use stdin)")
        }

        println("[hazel:keystore:add-user] Adding user $user with password ${"*".repeat(password.length)}")
        keystore.addUser(user, password)

        println("[hazel:keystore:add-user] Done!")
        keystore.close()
    }
}

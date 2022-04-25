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
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import de.mkammerer.argon2.Argon2Factory
import dev.floofy.hazel.core.KeystoreWrapper
import dev.floofy.hazel.data.KeystoreConfig

object EditUserPasswordKeystoreCommand: CliktCommand(
    name = "update-password",
    help = "Update a user's password (in case it got lost)"
) {
    private val username: String by argument("username", help = "The username to update the user's password.")
    private val uPassword: String by argument("password", help = "Password to update")

    private val path: String by option("--ks-path", "--path", "-p", help = "Returns the keystore path to use.")
        .required()

    private val password: String? by option("--password", "--ks-pwd", "--ps", help = "The keystore password that was used to save the keystore.", envvar = "HAZEL_KEYSTORE_PASSWORD")
    private val force: Boolean by option("--force", "-f", help = "The flag will forcefully delete the user, without a prompt.")
        .flag(default = false)

    override fun run() {
        val cwd = System.getProperty("user.dir", "")
        val home = System.getProperty("user.home", "/")

        val actualPath = when {
            path.startsWith("~/") -> home + path
            path.startsWith("./") -> (cwd + path.replaceFirstChar { "" }).trim()
            else -> path
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

        if (force) {
            keystore.deleteUser(username)
            keystore.addUser(username, uPassword)
            keystore.close()
        } else {
            when (prompt("Are you sure you want to update password for $username? [Y/n]: ", "n")) {
                null -> {
                    println("[hazel:keystore:delete-user] Cancelled from CTRL+C or some signal, exiting.")
                }

                "yes", "y", "Y", "Yes", "YES" -> {
                    keystore.deleteUser(username)
                    keystore.addUser(username, uPassword)
                    keystore.close()
                }

                "n", "N", "no", "No", "NO" -> {
                    println("[hazel:keystore:delete-user] Cancelled upon request.")
                }
            }
        }
    }
}

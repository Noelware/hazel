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
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import de.mkammerer.argon2.Argon2Factory
import dev.floofy.hazel.core.KeystoreWrapper
import dev.floofy.hazel.data.KeystoreConfig

object BaseKeystoreCliCommand: CliktCommand(
    name = "keystore",
    invokeWithoutSubcommand = true,
    help = """
    |The `keystore` subcommand is the management utility for managing the Hazel keystore which
    |is used for user authentication and secret key management.
    |
    |You can prepend a value (i.e, for S3) with `hazel keystore add <NAME>`.
    |You can view a list of keys by just executing the `keystore` subcommand without any arguments. :)
    """.trimMargin()
) {
    private val path: String? by option("--ks-path", "--path", "-p", help = "Returns the keystore path to use.")
        .default("./data/keystore.jks")

    private val password: String? by option("--password", "--ks-pwd", "--ps", help = "The keystore password that was used to save the keystore.", envvar = "HAZEL_KEYSTORE_PASSWORD")

    init {
        subcommands(
            AddUserToKeystoreCommand,
            AddValueToKeystoreCommand,
            CreateKeystoreCommand,
            DeleteUserFromKeystoreCommand
        )
    }

    override fun run() {
        if (currentContext.invokedSubcommand == null) {
            println("[hazel:keystore] Printing keys available...")

            val cwd = System.getProperty("user.dir", "")
            val home = System.getProperty("user.home", "/")

            val actualPath = when {
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

            val keys = keystore.list()
            for (key in keys) {
                val isUserKey = key.contains("users:")
                if (isUserKey) {
                    val username = key.split(":", limit = 2).last()
                    println("   ===> Found user $username! Use `hazel keystore update-password $username` to update their password.")
                } else {
                    println("   ===> $key=${"*".repeat(keystore[key].length)} (Use `hazel keystore update $key` to update the value!)")
                }
            }
        }
    }
}

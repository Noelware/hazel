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

object AddValueToKeystoreCommand: CliktCommand(
    name = "add",
    help = """
    |The `add` subcommands registers a key to the keystore, which can be read by the keystore wrapper.
    """.trimMargin()
) {
    private val name: String by argument("name", help = "The key to use to store")
    private val value: String? by argument("value", help = "The value to set. Use `--stdin` to populate this from stdin.")
        .optional()

    private val path: String? by option("--path", "--ks-path", "-p", help = "Returns the keystore path to use.").required()
    private val password: String? by option("--password", "--ks-pwd", "-ps", help = "The keystore password that was used to save the keystore.", envvar = "HAZEL_KEYSTORE_PASSWORD")
    private val stdin: Boolean? by option("--stdin", "-x", help = "If we should use the stdin to get the user's password.")
        .flag(default = false)

    override fun run() {
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

        val valueToSet: String
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

            valueToSet = String(passArray)

            // Close the stdin reader quietly
            reader.closeQuietly()
        } else {
            valueToSet = value ?: error("Missing `value` to set! (use `--stdin`/`-x` to use stdin)")
        }

        println("[hazel:keystore:add] Adding key $name with value ${"*".repeat(valueToSet.length)}")
        keystore.addValue(name, valueToSet)

        println("[hazel:keystore:add] Done!")
        keystore.close()
    }
}

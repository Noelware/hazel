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
import com.github.ajalt.clikt.parameters.types.file
import de.mkammerer.argon2.Argon2Factory
import dev.floofy.hazel.core.KeystoreWrapper
import dev.floofy.hazel.data.KeystoreConfig
import java.io.File

object CreateKeystoreCommand: CliktCommand(
    name = "create",
    help = "The `create` subcommand creates the keystore in the specified [path] and password to lock the keystore."
) {
    private val path: File by argument("path", help = "The path to create the keystore")
        .file(mustExist = false, canBeFile = true, canBeSymlink = true, canBeDir = false)

    private val password: String? by argument("password", help = "The password to lock the keystore in. This is a recommended option!")
        .optional()

    override fun run() {
        val keystore = KeystoreWrapper(
            KeystoreConfig(
                path.path,
                password
            ),
            Argon2Factory.create()
        )

        keystore.createIfNotExists()
        println("[hazel:keystore:create] Created keystore in path ${path.path}!")
    }
}

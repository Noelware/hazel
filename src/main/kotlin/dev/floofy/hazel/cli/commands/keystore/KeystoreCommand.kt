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

package dev.floofy.hazel.cli.commands.keystore

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintHelpMessage
import com.github.ajalt.clikt.core.subcommands

object KeystoreCommand: CliktCommand(
    name = "keystore",
    invokeWithoutSubcommand = true,
    help = """
    |The `keystore` command is the main management utility for managing Hazel's keystore.
    |
    |Hazel uses a Java keystore to handle secret key management, SSL certificates for the server,
    |and user persistence without bringing in an external database.
    |
    |To generate a keystore, you can run `hazel keystore generate` to create the keystore in the path
    |and you can load it using the `keystore.path` configuration key.
    """.trimMargin()
) {
    init {
        subcommands(ListKeysCommand)
    }

    override fun run() {
        if (currentContext.invokedSubcommand == null)
            throw PrintHelpMessage(this, false)
    }
}

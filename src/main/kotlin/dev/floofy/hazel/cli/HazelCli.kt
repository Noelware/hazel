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

package dev.floofy.hazel.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import dev.floofy.hazel.Bootstrap
import dev.floofy.hazel.HazelInfo
import dev.floofy.hazel.cli.commands.GenerateCommand
import dev.floofy.hazel.cli.commands.PingCommand
import dev.floofy.hazel.cli.commands.keystore.KeystoreCommand

object HazelCli: CliktCommand(
    name = "hazel",
    invokeWithoutSubcommand = true,
    allowMultipleSubcommands = true,
    help = """
    |Hazel is a simple, reliable Content Delivery Network (CDN) microservice to handle
    |your needs without doing anything to your data.
    |
    |This command line is the command line utility to manage the keystore for user persistence,
    |secret key storage, and managing the Hazel server.
    """.trimMargin()
) {
    private val configPath: String? by option(
        "-c", "--config",
        help = "The configuration path to run the server.",
        envvar = "HAZEL_CONFIG_PATH"
    )

    init {
        versionOption("v${HazelInfo.version} (${HazelInfo.commitHash} - ${HazelInfo.buildDate})", names = setOf("--version", "-v"))
        subcommands(
            PingCommand,
            GenerateCommand,
            KeystoreCommand
        )
    }

    override fun run() {
        // If we haven't invoked a subcommand with `hazel ...`, the server will run.
        if (currentContext.invokedSubcommand == null) {
            Bootstrap.bootstrap(configPath)
            throw ProgramResult(0)
        }
    }
}

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
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import dev.floofy.hazel.Bootstrap
import dev.floofy.hazel.HazelInfo
import dev.floofy.hazel.cli.keystore.BaseKeystoreCliCommand

object HazelCli: CliktCommand(
    name = "hazel",
    invokeWithoutSubcommand = true,
    allowMultipleSubcommands = true,
    help = """
    |Hazel is the main command line interface to interact with the Hazel keystore or the server.
    """.trimMargin()
) {
    private val configPath: String? by option(help = "The configuration path to run the server.", envvar = "HAZEL_CONFIG_PATH")

    init {
        versionOption("v${HazelInfo.version} (${HazelInfo.commitHash}) - ${HazelInfo.buildDate}")
        subcommands(GenerateCommand, PingCommand, BaseKeystoreCliCommand)
    }

    override fun run() {
        // This is needed because Clikt runs this before any subcommands are run, so
        // if we did "hazel keystore" without this condition, it will run the server
        // and not the subcommand's runner.
        if (currentContext.invokedSubcommand == null) {
            Bootstrap.bootstrap(configPath)
        }
    }
}

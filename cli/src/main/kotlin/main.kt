/*
 * 🪶 Hazel: Minimal, and fast HTTP proxy to host files from any cloud storage provider.
 * Copyright 2022-2023 Noelware <team@noelware.org>
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

@file:JvmName("CliMainKt")

package org.noelware.hazel.cli

import com.github.ajalt.clikt.completion.CompletionCommand
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyles
import com.github.ajalt.mordant.terminal.Terminal
import org.noelware.hazel.HazelInfo
import org.noelware.hazel.cli.commands.GenerateConfigCommand
import org.noelware.hazel.cli.commands.ServerCommand
import kotlin.system.exitProcess

private class HazelCli(terminal: Terminal): CliktCommand(
    help = "Command line runner for managing Hazel",
    name = "hazel",
    printHelpOnEmptyArgs = true,
    allowMultipleSubcommands = true
) {
    init {
        versionOption("${HazelInfo.version}+${HazelInfo.commitHash}") { version ->
            """
            |Hazel v$version (build date: ${HazelInfo.buildDate})
            |>> https://noelware.org/hazel | https://github.com/Noelware/hazel
            """.trimMargin("|")
        }

        subcommands(
            CompletionCommand(name = "completions"),
            GenerateConfigCommand(terminal),
            ServerCommand(terminal)
        )
    }

    override fun run() {}
}

fun main(args: Array<String>) {
    val terminal = Terminal()
    try {
        val cli = HazelCli(terminal)
        cli.main(args)
    } catch (e: Exception) {
        val urlColour = TextStyles.italic + TextColors.gray
        terminal.println(
            """
        |Unable to execute the main command line runner. If this is a reoccurring issue,
        |please report it to the Noelware team:
        |   ${urlColour("https://github.com/Noelware/hazel/issues/new")}
        """.trimMargin("|")
        )

        terminal.println()
        e.printStackTrace()
        exitProcess(1)
    }
}

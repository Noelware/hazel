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

package dev.floofy.hazel.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import java.nio.file.Files
import java.nio.file.Paths

object GenerateCommand: CliktCommand(
    name = "generate",
    help = """
    |The "generate" subcommand generates a configuration file that can be executed to run the server.
    |
    |The `dest` argument is the source to put the file in. The file cannot be a directory since Hazel
    |loads it and runs it.
    """.trimMargin()
) {
    private val dest by argument(
        "dest",
        help = "The destination of the file to output in"
    ).file(mustExist = false, canBeFile = true, canBeDir = false)

    override fun run() {
        if (dest.exists()) {
            println("[hazel:generate] File ${dest.path} already exists.")
            return
        }

        println("[hazel:generate] Writing output to ${dest.path}...")
        val contents = """
        |[keystore]
        |file = "./data/keystore.jks"
        |
        |[storage]
        |class = "fs"
        |
        |[storage.fs]
        |directory = "./data/hazel"
        """.trimMargin()

        Files.write(Paths.get(dest.toURI()), contents.toByteArray())
        println("[hazel:generate] Generated a default configuration file in ${dest.path}!")
    }
}

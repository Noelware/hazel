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
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

object GenerateCommand: CliktCommand(
    name = "generate",
    help = """
    |The "generate" subcommand generates a configuration file that can be executed to run the server.
    |
    |The `SRC` argument is the source to put the file in. The file cannot be a directory since Hazel
    |loads it and runs it.
    """.trimMargin()
) {
    private val src: File by argument(help = "Returns the source file to use. :)")
        .file(canBeFile = true, canBeDir = false, canBeSymlink = true, mustExist = false)

    override fun run() {
        if (src.exists()) {
            println("[hazel:generate] File ${src.path} already exists.")
            return
        }

        println("[hazel:generate] Writing output to ${src.path}...")
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

        Files.write(Paths.get(src.toURI()), contents.toByteArray())
        println("[hazel:generate] Generated a default configuration file in ${src.path}!")
    }
}

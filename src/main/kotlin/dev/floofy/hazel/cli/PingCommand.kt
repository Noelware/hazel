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
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.long
import java.io.File

object PingCommand: CliktCommand(
    name = "ping",
    help = """
    |The "hazel ping" command executes a HTTP GET request to the HTTP service (if it is running),
    |the metadata is from the configuration path (which by default, will use ./config.toml OR under the
    |`HAZEL_CONFIG_PATH` environment variable)
    |
    |This is useful to setup monitoring with Hazel with Logstash (under the `command` input) or checking the pod
    |availability with Kubernetes.
    """.trimMargin()
) {
    private val configPath: File? by option("-c", "--config", "--path", help = "The configuration path to use for the connection", envvar = "HAZEL_CONFIG_PATH")
        .file(mustExist = true, mustBeReadable = true)
        .default(File("./config.toml"))

    private val timeout: Long? by option("--timeout", "-t", help = "How long to wait in seconds to complete the request.")
        .long()
        .default(5)

    override fun run() {
        println("[hazel:ping] Now pinging server... (timeout=$timeout, configPath=${configPath!!.path})")
    }
}

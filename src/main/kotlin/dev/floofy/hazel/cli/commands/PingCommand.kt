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
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.arguments.argument
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking

object PingCommand: CliktCommand(
    name = "ping",
    help = "Pings the server with the [URL] provided."
) {
    private val url: String by argument(
        "url",
        help = "The URL of the server to hit"
    )

    override fun run() {
        val client = HttpClient(OkHttp)
        val res = runBlocking {
            client.get("$url/heartbeat")
        }

        if (!res.status.isSuccess()) {
            throw CliktError("Couldn't reach server at $url, is it open?")
        }

        val body = runBlocking {
            res.bodyAsText()
        }

        if (body != "OK") {
            throw CliktError("Server running at $url is not a Hazel server! (GET /heartbeat = OK)")
        }
    }
}

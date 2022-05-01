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

package dev.floofy.hazel.routing.endpoints

import dev.floofy.hazel.core.StorageWrapper
import dev.floofy.hazel.data.Config
import dev.floofy.hazel.routing.AbstractEndpoint
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.apache.commons.lang3.time.StopWatch
import java.util.concurrent.TimeUnit

class ListFilesEndpoint(private val trailer: StorageWrapper, private val config: Config): AbstractEndpoint("/list") {
    override suspend fun call(call: ApplicationCall) {
        val stopwatch = StopWatch.createStarted()
        val files = trailer.listAll()
        stopwatch.stop()

        val baseUrl = config.baseUrl.ifEmpty {
            "http://${config.server.host}:${config.server.port}"
        }

        val payload = buildJsonObject {
            put("success", true)
            put(
                "data",
                buildJsonObject {
                    put("took", stopwatch.getTime(TimeUnit.MILLISECONDS))

                    for (file in files) {
                        put(
                            file.name,
                            buildJsonObject {
                                put("endpoint", "$baseUrl/${file.name}")
                                put("name", file.name)
                                put("content_type", file.contentType)
                                put("will_prompt_download", file.contentType.startsWith("application/octet-stream"))
                                put("size", file.size.toDouble())
                                put("created_at", file.createdAt.toString())
                            }
                        )
                    }
                }
            )
        }

        call.respond(HttpStatusCode.OK, payload)
    }
}

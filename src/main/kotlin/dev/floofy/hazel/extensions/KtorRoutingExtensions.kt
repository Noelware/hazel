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

package dev.floofy.hazel.extensions

import dev.floofy.hazel.HazelScope
import dev.floofy.hazel.core.StorageWrapper
import dev.floofy.utils.koin.inject
import dev.floofy.utils.slf4j.logging
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import io.ktor.utils.io.pool.ByteBufferPool
import io.sentry.Sentry
import io.sentry.kotlin.SentryContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.noelware.remi.core.figureContentType
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

fun Route.resourcePath(path: String): Route {
    val storage by inject<StorageWrapper>()
    val log by logging("dev.floofy.hazel.extensions.KtorRoutingExtensionsKt")

    return get(path) {
        log.info("was i called?")
        val clsLoader = Thread.currentThread().contextClassLoader
        val stream = clsLoader.getResourceAsStream(path)

        if (stream == null) {
            call.respond(
                HttpStatusCode.NotFound,
                buildJsonObject {
                    put("success", false)
                    put(
                        "errors",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("code", "UNKNOWN_FILE")
                                    put("message", "Cannot retrieve input stream for $path.")
                                }
                            )
                        }
                    )
                }
            )

            return@get
        }

        // Create a copy of this stream (so it can check the content type)
        val baos = ByteArrayOutputStream()
        withContext(Dispatchers.IO) {
            stream.transferTo(baos)
        }

        // Get the data as a byte array
        val data = baos.toByteArray()
        val contentTypeStream = ByteArrayInputStream(data)
        val contentType = ContentType.parse(storage.trailer.figureContentType(contentTypeStream))

        call.respond(
            HttpStatusCode.OK,
            object: OutgoingContent.ReadChannelContent() {
                override val contentType: ContentType = contentType
                override val contentLength: Long = data.size.toLong()

                override fun readFrom(): ByteReadChannel {
                    return ByteArrayInputStream(data).toByteReadChannel(
                        ByteBufferPool(4092, 8192),
                        if (Sentry.isEnabled()) SentryContext() + HazelScope.coroutineContext else HazelScope.coroutineContext
                    )
                }
            }
        )
    }
}

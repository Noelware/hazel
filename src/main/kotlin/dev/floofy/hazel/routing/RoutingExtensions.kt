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

package dev.floofy.hazel.routing

import dev.floofy.hazel.HazelScope
import dev.floofy.hazel.core.StorageWrapper
import dev.floofy.hazel.extensions.inject
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import io.ktor.utils.io.pool.*
import io.ktor.utils.io.pool.ByteBufferPool
import io.sentry.Sentry
import io.sentry.kotlin.SentryContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.noelware.remi.core.CHECK_WITH
import org.noelware.remi.filesystem.FilesystemStorageConfig
import org.noelware.remi.filesystem.FilesystemStorageTrailer
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

suspend fun Routing.createCdnEndpoints() {
    val storage by inject<StorageWrapper>()
    val log = LoggerFactory.getLogger("dev.floofy.hazel.routing.CdnRoutingExtensionKt")

    log.info("Configuring CDN endpoints...")
    val contents = storage.listAll()

    for (content in contents) {
        val name: String = if (storage.trailer is FilesystemStorageTrailer) {
            content.name
                .replace(System.getProperty("user.dir", ""), "")
                .replace((storage.trailer.config as FilesystemStorageConfig).directory, "")
        } else {
            content.name
        }

        log.debug("Found file $name to register!")
        get("/$name") {
            val stream = storage.open(if (storage.trailer is FilesystemStorageTrailer) "./$name" else name)
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
                                        put("message", "Cannot retrieve input stream for $name.")
                                    }
                                )
                            }
                        )
                    }
                )

                return@get
            }

            // Create a clone of the stream (so we can get the content length)
            val baos = ByteArrayOutputStream()
            withContext(Dispatchers.IO) {
                stream.transferTo(baos)
            }

            val data = baos.toByteArray()
            val newStream = ByteArrayInputStream(data)

            val rawCt = if (content.contentType == CHECK_WITH)
                storage.findContentType(newStream)
            else
                content.contentType

            val contentType = ContentType.parse(rawCt)
            val shouldDownload = when {
                call.request.queryParameters["download"] != null -> true
                rawCt.startsWith("application/octet-stream") -> true
                else -> false
            }

            if (shouldDownload) {
                if (!call.response.isCommitted) {
                    call.response.header(
                        HttpHeaders.ContentDisposition,
                        "attachment; filename=\"${name.split("/").last()}\""
                    )
                }
            }

            if (shouldDownload) {
                // close the stream so we don't leak
                withContext(Dispatchers.IO) {
                    stream.close()
                }

                call.respond(HttpStatusCode.NoContent)
            } else {
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
    }
}

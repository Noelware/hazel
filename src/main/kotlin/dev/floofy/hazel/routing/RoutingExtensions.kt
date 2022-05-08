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
import dev.floofy.hazel.core.ImageManipulator
import dev.floofy.hazel.core.StorageWrapper
import dev.floofy.utils.koin.*
import dev.floofy.utils.slf4j.logging
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
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
import org.noelware.remi.core.CHECK_WITH
import org.noelware.remi.filesystem.FilesystemStorageConfig
import org.noelware.remi.filesystem.FilesystemStorageTrailer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

suspend fun Routing.createCdnEndpoints() {
    val storage by inject<StorageWrapper>()
    val log by logging("dev.floofy.hazel.routing.CreateCdnEndpointsKt")

    log.debug("Configuring CDN endpoints...")
    val contents = storage.listAll()
    log.debug("Found ${contents.size} items to create as routes!")

    for (content in contents) {
        val name = if (storage.trailer is FilesystemStorageTrailer) {
            content.name
                .replace(System.getProperty("user.dir", ""), "")
                .replace((storage.trailer.config as FilesystemStorageConfig).directory, "")
        } else {
            content.name
        }

        log.debug("Found route $name to register!")
        get("/$name") {
            callOnRoute(content, call, storage, name)
        }
    }
}

private suspend fun onImage(
    call: ApplicationCall,
    contentType: ContentType,
    writerType: String,
    stream: ByteArrayInputStream
) {
    val resize = call.request.queryParameters["resize"]
    val format = call.request.queryParameters["format"]
    var done = false

    if (resize != null && resize.toIntOrNull() != null) {
        val resizeBy = resize.toInt()
        val (width, height) = when (resizeBy) {
            256 -> Pair(256, 256)
            512 -> Pair(512, 512)
            1024 -> Pair(1024, 1024)
            2048 -> Pair(2048, 2048)
            else -> Pair(0, 0)
        }

        if (width == 0 || height == 0) {
            call.respond(
                HttpStatusCode.BadRequest,
                buildJsonObject {
                    put("success", false)
                    put(
                        "errors",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("code", "INVALID_RESIZE_PARAMS")
                                    put("message", "The ?resize parameters can only accept 256, 512, 1024, and 2048.")
                                }
                            )
                        }
                    )
                }
            )

            done = true
        }

        if (done) return

        val bytes = ImageManipulator.resize(stream, writerType, width, height)
        call.respond(
            HttpStatusCode.OK,
            object: OutgoingContent.ReadChannelContent() {
                override val contentType: ContentType = contentType
                override val contentLength: Long = bytes.size.toLong()

                override fun readFrom(): ByteReadChannel = ByteArrayInputStream(bytes).toByteReadChannel(
                    ByteBufferPool(4092, 8192),
                    if (Sentry.isEnabled()) SentryContext() + HazelScope.coroutineContext else HazelScope.coroutineContext
                )
            }
        )

        done = true
    }

    if (format != null && !done) {
        val actualFormat = when (format) {
            "png" -> "png"
            "jpeg", "jpg" -> "jpg"
            else -> null
        }

        if (actualFormat == null) {
            call.respond(
                HttpStatusCode.BadRequest,
                buildJsonObject {
                    put("success", false)
                    put(
                        "errors",
                        buildJsonArray {
                            add(
                                buildJsonObject {
                                    put("code", "INVALID_FORMAT")
                                    put("message", "We only accept jpg and png transformation.")
                                }
                            )
                        }
                    )
                }
            )

            return
        }

        val bytes = ImageManipulator.format(stream, actualFormat)
        val newContentType = ContentType.parse("image/$actualFormat")
        call.respond(
            HttpStatusCode.OK,
            object: OutgoingContent.ReadChannelContent() {
                override val contentType: ContentType = newContentType
                override val contentLength: Long = bytes.size.toLong()

                override fun readFrom(): ByteReadChannel = ByteArrayInputStream(bytes).toByteReadChannel(
                    ByteBufferPool(4092, 8192),
                    if (Sentry.isEnabled()) SentryContext() + HazelScope.coroutineContext else HazelScope.coroutineContext
                )
            }
        )

        return
    }
}

private suspend fun callOnRoute(
    obj: org.noelware.remi.core.Object,
    call: ApplicationCall,
    storage: StorageWrapper,
    name: String
) {
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

        return
    }

    // Create a copy of this stream (so it can check the content type)
    val baos = ByteArrayOutputStream()
    withContext(Dispatchers.IO) {
        stream.transferTo(baos)
    }

    // Get the data as a byte array
    val data = baos.toByteArray()
    val contentTypeStream = ByteArrayInputStream(data)
    val rawContentType = if (obj.contentType == CHECK_WITH) storage.findContentType(contentTypeStream) else obj.contentType

    // NOTE(noel): `contentTypeStream` is now exhausted and can no longer be used.

    val contentType = ContentType.parse(rawContentType)
    val shouldDownload = when {
        contentType.match(ContentType.Application.GZip) -> true
        contentType.match(ContentType.Application.OctetStream) -> true
        contentType.match(ContentType.Application.Zip) -> true
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

    // Check if it is an image, if so, let's do some image manipulation!
    val isImage = when (contentType) {
        ContentType.Image.JPEG -> true
        ContentType.Image.PNG -> true
        else -> false
    }

    if (isImage && !call.request.queryParameters.isEmpty()) {
        val newImageStream = ByteArrayInputStream(data)
        val format = when (contentType) {
            ContentType.Image.PNG -> "png"
            ContentType.Image.JPEG -> "jpeg"
            else -> error("this should never happen")
        }

        return onImage(call, contentType, format, newImageStream)
    }

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

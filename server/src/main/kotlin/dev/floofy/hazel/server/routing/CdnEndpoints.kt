package dev.floofy.hazel.server.routing

import dev.floofy.hazel.server.HazelScope
import dev.floofy.hazel.server.StorageWrapper
import dev.floofy.hazel.server.util.ImageManipulator
import dev.floofy.utils.koin.inject
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
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.noelware.remi.core.CHECK_WITH
import org.noelware.remi.filesystem.FilesystemStorageConfig
import org.noelware.remi.filesystem.FilesystemStorageTrailer
import java.io.ByteArrayInputStream

suspend fun Routing.createCdnEndpoints() {
    val storage by inject<StorageWrapper>()
    val log by logging("dev.floofy.hazel.routing.CdnEndpointsKt")

    log.debug("Now configuring CDN endpoints...")
    val contents = storage.listAll()
    log.debug("Found ${contents.size} objects to create as routes!")

    for (content in contents) {
        val name = if (storage.trailer is FilesystemStorageTrailer) {
            content.name
                .replace(System.getProperty("user.dir", ""), "")
                .replace((storage.trailer.config as FilesystemStorageConfig).directory, "")
        } else {
            content.name
        }

        log.debug("Registering route /$name and /$name?json")
        get("/$name") {
            callOnRoute(content, call, storage, name)
        }
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
                putJsonArray("errors") {
                    addJsonObject {
                        put("code", "UNKNOWN_FILE")
                        put("message", "Cannot retrieve input stream for $name.")
                    }
                }
            }
        )

        return
    }

    val data = withContext(Dispatchers.IO) {
        stream.readAllBytes()
    }

    val rawCt = if (obj.contentType == CHECK_WITH) storage.findContentType(ByteArrayInputStream(data)) else obj.contentType
    val contentType = ContentType.parse(rawCt)

    val isImage = when (contentType) {
        ContentType.Image.PNG, ContentType.Image.JPEG -> true
        else -> false
    }

    if (isImage && !call.request.queryParameters.isEmpty()) {
        val stream = ByteArrayInputStream(data)
        val format = when (contentType) {
            ContentType.Image.PNG -> "png"
            ContentType.Image.JPEG -> "jpeg"
            else -> error("this should never happen")
        }

        return onImage(call, contentType, format, stream)
    }

    call.respond(
        HttpStatusCode.OK,
        object: OutgoingContent.ReadChannelContent() {
            override val contentLength: Long = data.size.toLong()
            override val contentType: ContentType = contentType
            override fun readFrom(): ByteReadChannel =
                ByteArrayInputStream(data).toByteReadChannel(
                    ByteBufferPool(4092, 8192),
                    if (Sentry.isEnabled()) SentryContext() + HazelScope.coroutineContext else HazelScope.coroutineContext
                )
        }
    )
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
                    putJsonArray("errors") {
                        addJsonObject {
                            put("code", "INVALID_RESIZE_PARAMS")
                            put("message", "The ?resize parameters can only accept 256, 512, 1024, and 2048.")
                        }
                    }
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
                    putJsonArray("errors") {
                        addJsonObject {
                            put("code", "INVALID_FORMAT")
                            put("message", "We only accept jpg and png transformation.")
                        }
                    }
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

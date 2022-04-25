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

package dev.floofy.hazel.core

import dev.floofy.hazel.data.StorageClass
import dev.floofy.hazel.data.StorageConfig
import dev.floofy.hazel.extensions.formatSize
import gay.floof.utils.slf4j.logging
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.apache.commons.lang3.time.StopWatch
import org.noelware.remi.core.CHECK_WITH
import org.noelware.remi.core.StorageTrailer
import org.noelware.remi.core.figureContentType
import org.noelware.remi.filesystem.FilesystemStorageTrailer
import org.noelware.remi.minio.MinIOStorageTrailer
import org.noelware.remi.s3.S3StorageTrailer
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * Wrapper for configuring the storage trailer that **hazel** will use.
 */
class StorageWrapper(config: StorageConfig) {
    private val trailer: StorageTrailer<*>
    private val log by logging<StorageWrapper>()

    init {
        log.info("Figuring out what storage trailer to use...")

        trailer = when (config.storageClass) {
            StorageClass.FS -> {
                assert(config.fs != null) { "Configuration for the local disk is missing." }

                FilesystemStorageTrailer(config.fs!!.directory)
            }

            StorageClass.FILESYSTEM -> {
                assert(config.filesystem != null) { "Configuration for the local disk is missing." }

                FilesystemStorageTrailer(config.filesystem!!.directory)
            }

            StorageClass.S3 -> {
                assert(config.s3 != null) { "Configuration for Amazon S3 is missing." }

                S3StorageTrailer(config.s3!!)
            }

            StorageClass.MINIO -> {
                assert(config.minio != null) { "Configuration for MinIO is missing." }

                MinIOStorageTrailer(config.minio!!)
            }
        }

        log.info("Using storage trailer ${config.storageClass}!")

        // block the main thread so the trailer can be
        // loaded successfully.
        runBlocking {
            try {
                log.info("Starting up storage trailer...")
                trailer.init()
            } catch (e: Exception) {
                if (e is IllegalStateException && e.message?.contains("doesn't support StorageTrailer#init/0") == true)
                    return@runBlocking

                throw e
            }
        }
    }

    /**
     * Opens a file under the [path] and returns the [InputStream] of the file.
     */
    suspend fun open(path: String): InputStream? = trailer.open(path)

    /**
     * Deletes the file under the [path] and returns a [Boolean] if the
     * operation was a success or not.
     */
    suspend fun delete(path: String): Boolean = trailer.delete(path)

    /**
     * Checks if the file exists under this storage trailer.
     * @param path The path to find the file.
     */
    suspend fun exists(path: String): Boolean = trailer.exists(path)

    /**
     * Uploads file to this storage trailer and returns a [Boolean] result
     * if the operation was a success or not.
     *
     * @param path The path to upload the file to
     * @param stream The [InputStream] that represents the raw data.
     * @param contentType The content type of the file (useful for S3 and GCS support)!
     */
    suspend fun upload(
        path: String,
        stream: InputStream,
        contentType: String = "application/octet-stream"
    ): Boolean = trailer.upload(path, stream, contentType)

    suspend fun listAll(): List<org.noelware.remi.core.Object> = trailer.listAll()

    suspend fun addRoutesBasedOffFiles(routing: Routing) {
        val stopwatch = StopWatch.createStarted()
        val files = trailer.listAll()
        log.info("Took ${stopwatch.getTime(TimeUnit.MILLISECONDS)}ms to collect information. :)")

        for (file in files) {
            println("file ${file.name}:")
            println("   contentType: ${file.contentType}")
            println("   createdAt:   ${file.createdAt}")
            println("   size:        ${file.size.formatSize()}")

            routing.route("/${file.name}", HttpMethod.Get) {
                handle {
                    val stream = if (file.inputStream != null) file.inputStream else trailer.open(file.name)
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
                                                put("message", "Cannot retrieve input stream for ${file.name}.")
                                            }
                                        )
                                    }
                                )
                            }
                        )

                        return@handle
                    }

                    val contentType = if (file.contentType == CHECK_WITH) {
                        trailer.figureContentType(stream)
                    } else {
                        file.contentType
                    }

                    val shouldDownload = when {
                        call.request.queryParameters["download"] != null -> {
                            val download = call.request.queryParameters["download"]
                            download != null
                        }

                        contentType.startsWith("application/octet-stream") -> true
                        else -> false
                    }

                    val ktorContentType = ContentType.parse(file.contentType)
                    if (shouldDownload) {
                        call.response.header(HttpHeaders.ContentDisposition, "attachment; filename=\"${file.name.split("/").last()}\"")
                    }

                    call.respondOutputStream(ktorContentType, HttpStatusCode.OK) {
                        stream.use { `is` ->
                            `is`.transferTo(this)
                        }
                    }
                }
            }

            routing.route("/${file.name}", HttpMethod.Delete) {
                authenticate("hazel") {
                    handle {
                        val path = call.request.uri
                        val result = delete(path.substring(1))

                        if (result) {
                            call.respond(
                                HttpStatusCode.OK,
                                buildJsonObject {
                                    put("success", true)
                                }
                            )
                        } else {
                            call.respond(
                                HttpStatusCode.BadRequest,
                                buildJsonObject {
                                    put("success", false)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

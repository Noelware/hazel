/*
 * 🪶 Hazel: Minimal, and fast HTTP proxy to host files from any cloud storage provider.
 * Copyright 2022-2023 Noelware <team@noelware.org>
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

package org.noelware.hazel.server.endpoints

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.http.*
import io.ktor.server.response.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import io.ktor.utils.io.pool.ByteBufferPool
import kotlinx.serialization.Serializable
import org.noelware.hazel.HazelInfo
import org.noelware.hazel.HazelScope
import org.noelware.hazel.data.ApiResponse
import org.noelware.hazel.modules.storage.StorageDriver
import org.noelware.ktor.endpoints.AbstractEndpoint
import org.noelware.ktor.endpoints.Get
import org.noelware.remi.support.filesystem.FilesystemStorageService
import java.io.InputStream

@Serializable
private data class MainResponse(
    val message: String = "Hello, world!",
    val docs: String = "https://noelware.org/docs/hazel/server/${HazelInfo.version}"
)

private fun <T: InputStream> createKtorContentWithInputStream(
    `is`: T,
    contentType: ContentType,
    contentLength: Long = `is`.available().toLong(),
    status: HttpStatusCode = HttpStatusCode.OK
): OutgoingContent.ReadChannelContent {
    check(contentLength != 0L) { "Content-Length can't be 0" }
    return object: OutgoingContent.ReadChannelContent() {
        override val contentType: ContentType = contentType
        override val contentLength: Long = contentLength
        override val status: HttpStatusCode = status
        override fun readFrom(): ByteReadChannel = `is`.toByteReadChannel(
            ByteBufferPool(4092, 8192),
            HazelScope.coroutineContext
        )
    }
}

class MainEndpoint(private val storage: StorageDriver): AbstractEndpoint("/") {
    @Get
    suspend fun main(call: ApplicationCall) = call.respond(HttpStatusCode.OK, ApiResponse.ok(MainResponse()))

    @Get("/{params...}")
    suspend fun catchAll(call: ApplicationCall) {
        val paths = (call.parameters.getAll("params") ?: listOf()).joinToString("/")
        val searchPath = if (storage.service is FilesystemStorageService) {
            "./$paths"
        } else {
            paths
        }

        val blob = storage.blob(searchPath) ?: return call.respond(HttpStatusCode.NotFound)
        if (blob.createdAt() != null) call.response.header("X-File-Created-At", blob.createdAt()!!.toHttpDateString())
        if (blob.lastModifiedAt() != null) call.response.header("X-File-Last-Modified", blob.lastModifiedAt()!!.toHttpDateString())
        if (blob.etag() != null) call.response.header("Etag", blob.etag()!!)

        val contentType = ContentType.parse(blob.contentType() ?: "application/octet-stream")
        call.respond(createKtorContentWithInputStream(blob.inputStream()!!, contentType))
    }
}

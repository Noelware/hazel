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

package dev.floofy.hazel.streaming

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import java.io.InputStream

class LocalInputStreamContent<I>(
    private val stream: I,
    override val contentType: ContentType,
    private val actualSize: Long = 0
): OutgoingContent.ReadChannelContent() where I: InputStream {
    override val contentLength: Long = actualSize
    override fun readFrom(): ByteReadChannel = stream.toByteReadChannel()
}

suspend fun <I: InputStream> ApplicationCall.respondInputStream(statusCode: HttpStatusCode = HttpStatusCode.OK, contentType: ContentType = ContentType.Application.OctetStream, stream: I) {
    val content = LocalInputStreamContent(stream, contentType)
    respond(statusCode, content)
}

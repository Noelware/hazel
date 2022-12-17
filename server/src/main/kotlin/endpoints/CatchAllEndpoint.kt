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
import io.ktor.server.response.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import io.ktor.utils.io.pool.ByteBufferPool
import org.noelware.hazel.HazelScope
import java.io.ByteArrayInputStream

private fun createKtorContent(
    bytes: ByteArray,
    contentType: ContentType,
    contentLength: Long = bytes.size.toLong(),
    status: HttpStatusCode = HttpStatusCode.OK
): OutgoingContent.ReadChannelContent {
    check(contentLength != 0L) { "Content-Length can't be 0" }
    return object: OutgoingContent.ReadChannelContent() {
        override val contentType: ContentType = contentType
        override val contentLength: Long = contentLength
        override val status: HttpStatusCode = status
        override fun readFrom(): ByteReadChannel = ByteArrayInputStream(bytes).toByteReadChannel(
            ByteBufferPool(4092, 8192),
            HazelScope.coroutineContext
        )
    }
}

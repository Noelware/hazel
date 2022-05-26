@file:Suppress("UNUSED")
package dev.floofy.hazel.server.routing

import dev.floofy.hazel.server.HazelScope
import dev.floofy.hazel.server.config.Config
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import io.ktor.utils.io.pool.ByteBufferPool
import io.sentry.Sentry
import io.sentry.kotlin.SentryContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.noelware.ktor.endpoints.AbstractEndpoint
import org.noelware.ktor.endpoints.Get
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class MainEndpoint(private val config: Config): AbstractEndpoint() {
    @Get
    suspend fun main(call: ApplicationCall) {
        if (config.frontend) {
            if (call.request.queryParameters["json"] != null) {
                call.respond(
                    HttpStatusCode.OK,
                    buildJsonObject {
                        put("success", true)
                        put("message", "hello world!")
                    }
                )

                return
            }

            val resource = this::class.java.getResourceAsStream("/index.html")!!
            val baos = ByteArrayOutputStream()

            withContext(Dispatchers.IO) {
                resource.transferTo(baos)
            }

            val data = baos.toByteArray()
            call.respond(
                HttpStatusCode.OK,
                object: OutgoingContent.ReadChannelContent() {
                    override val contentLength: Long = data.size.toLong()
                    override val contentType: ContentType = ContentType.Text.Html
                    override fun readFrom(): ByteReadChannel =
                        ByteArrayInputStream(data).toByteReadChannel(
                            ByteBufferPool(4092, 8192),
                            if (Sentry.isEnabled()) SentryContext() + HazelScope.coroutineContext else HazelScope.coroutineContext
                        )
                }
            )

            return
        }

        call.respond(
            HttpStatusCode.OK,
            buildJsonObject {
                put("success", true)
                put("message", "hello world!")
            }
        )
    }
}

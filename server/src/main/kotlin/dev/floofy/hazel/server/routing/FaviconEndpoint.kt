@file:Suppress("UNUSED")
package dev.floofy.hazel.server.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import org.noelware.ktor.endpoints.AbstractEndpoint
import org.noelware.ktor.endpoints.Get
import java.io.File

class FaviconEndpoint: AbstractEndpoint("/favicon.ico") {
    @Get
    suspend fun call(call: ApplicationCall) {
        call.response.status(HttpStatusCode.OK)
        call.respondFile(File("./assets/feather.png"))
    }
}


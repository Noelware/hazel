@file:Suppress("UNUSED")
package dev.floofy.hazel.server.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import org.noelware.ktor.endpoints.AbstractEndpoint
import org.noelware.ktor.endpoints.Get

class HeartbeatEndpoint: AbstractEndpoint("/heartbeat") {
    @Get
    suspend fun call(call: ApplicationCall) {
        call.respond(HttpStatusCode.OK, "OK")
    }
}

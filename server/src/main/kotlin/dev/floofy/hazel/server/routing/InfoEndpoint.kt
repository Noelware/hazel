@file:Suppress("UNUSED")
package dev.floofy.hazel.server.routing

import dev.floofy.hazel.server.HazelInfo
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.noelware.ktor.endpoints.AbstractEndpoint
import org.noelware.ktor.endpoints.Get

class InfoEndpoint: AbstractEndpoint("/info") {
    @Get
    suspend fun call(call: ApplicationCall) {
        call.respond(
            HttpStatusCode.OK,
            buildJsonObject {
                put("success", true)
                put(
                    "data",
                    buildJsonObject {
                        put("version", HazelInfo.version)
                        put("commit_sha", HazelInfo.commitHash)
                        put("build_date", HazelInfo.buildDate)
                        put("distribution", HazelInfo.distributionType.key)

                        if (HazelInfo.dediNode != null)
                            put("dedi_node", HazelInfo.dediNode)
                    }
                )
            }
        )
    }
}

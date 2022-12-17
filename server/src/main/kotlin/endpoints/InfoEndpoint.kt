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
import io.ktor.server.application.*
import io.ktor.server.response.*
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.noelware.hazel.HazelInfo
import org.noelware.hazel.data.ApiResponse
import org.noelware.ktor.endpoints.AbstractEndpoint
import org.noelware.ktor.endpoints.Get

@Serializable
private data class InfoResponse(
    val version: String,

    @SerialName("build_date")
    val buildDate: Instant,

    @SerialName("commit_hash")
    val commitHash: String
)

class InfoEndpoint: AbstractEndpoint("/info") {
    @Get
    suspend fun main(call: ApplicationCall) = call.respond(
        HttpStatusCode.OK,
        ApiResponse.ok(
            InfoResponse(
                HazelInfo.version,
                HazelInfo.buildDate,
                HazelInfo.commitHash
            )
        )
    )
}
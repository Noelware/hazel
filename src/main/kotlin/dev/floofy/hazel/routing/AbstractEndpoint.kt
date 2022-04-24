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

package dev.floofy.hazel.routing

import io.ktor.http.*
import io.ktor.server.application.*

/**
 * Represents an endpoint that resides on the server.
 * @param path The path to use, must start with `/`
 * @param method the [HttpMethod] to use for this method.
 */
abstract class AbstractEndpoint(val path: String, val methods: List<HttpMethod> = listOf(HttpMethod.Get)) {
    /**
     * Method to call when the route is being executed.
     * @param call The main [ApplicationCall] that is used when handled.
     */
    abstract suspend fun call(call: ApplicationCall)
}

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

package dev.floofy.hazel.plugins

import dev.floofy.hazel.extensions.ifNotNull
import io.ktor.server.application.*
import io.ktor.server.request.*
import org.slf4j.MDC

val UserAgentPlugin = createApplicationPlugin("NinoUserAgentPlugin") {
    onCall { c ->
        val userAgent = c.request.userAgent()
        userAgent.ifNotNull {
            MDC.put("user_agent", it)
        }
    }

    onCallReceive { _, _ ->
        MDC.remove("user_agent")
    }
}

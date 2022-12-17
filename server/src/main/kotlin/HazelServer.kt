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

package org.noelware.hazel.server

import io.ktor.server.application.*
import io.ktor.server.engine.*
import java.io.Closeable

interface HazelServer: Closeable {
    /**
     * Checks if the server has started or not.
     */
    val started: Boolean

    /**
     * The application engine that Ktor is using for the server.
     */
    val server: ApplicationEngine

    /**
     * Extension function to tailor the application module for this [HazelServer]
     * instance.
     */
    fun Application.module()

    /**
     * Starts the server, this will be a no-op if [started] was already
     * set to `true`.
     */
    fun start()
}

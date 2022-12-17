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

package org.noelware.hazel.server.bootstrap

import java.io.File

/**
 * Represents a phase in the server's bootstrapping. This will plug into the [org.noelware.hazel.server.Bootstrap.run] call
 * and does pre-initialization.
 */
abstract class BootstrapPhase {
    abstract fun bootstrap(configPath: File)

    companion object {
        internal val PHASES: List<BootstrapPhase> = listOf(
            PreInitPhase,
            ConfigureModulesPhase,
            RunServerPhase
        )
    }
}

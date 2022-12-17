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

package org.noelware.hazel.configuration.host

import org.noelware.hazel.configuration.kotlin.dsl.Config
import java.io.File

/**
 * Represents a host interface to implement loading the configuration file from a path.
 */
interface ConfigurationHost {
    /**
     * Loads the configuration file in the given [path]
     * @param path the file to load from
     * @return the [Config] or `null` if it couldn't be found
     */
    fun load(path: File): Config
}

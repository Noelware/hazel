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

package org.noelware.hazel

import dev.floofy.utils.kotlin.ifNotNull
import kotlinx.datetime.Instant
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*

@OptIn(ExperimentalSerializationApi::class)
object HazelInfo {
    /**
     * Returns the [distribution type][Distribution] of the Bamboo server.
     */
    @JvmStatic
    val distribution: Distribution = Distribution.detect()

    /** Returns true if this distribution is a nightly build or not. */
    @JvmStatic
    val isNightlyBuild: Boolean

    /**
     * Represents the date when Hazel was built at.
     */
    @JvmStatic
    val buildDate: Instant

    /**
     * Returns the Git commit hash from Hazel's [GitHub repository](https://github.com/Noelware/hazel)
     */
    @JvmStatic
    val commitHash: String

    /**
     * Returns the version of Hazel
     */
    @JvmStatic
    val version: String

    /**
     * Returns the dedicated node name for this current build.
     */
    @JvmStatic
    val dedicatedNode: String? by lazy {
        // Check if we have `NODE_NAME` in the environment variables, this is defined
        // in the Helm Chart or by the Noel on Cloud Kubernetes operator
        val fromK8sMeta = System.getenv("NODE_NAME")
        if (fromK8sMeta != null) return@lazy fromK8sMeta

        // In Noelware, we define the dedicated node name by the `WINTERFOX_DEDI_NODE`
        // environment variable.
        val prodEnv = System.getenv("WINTERFOX_DEDI_NODE")
        if (prodEnv != null) return@lazy prodEnv

        null
    }

    init {
        val stream = this::class.java.getResourceAsStream("/build-info.json")
            ?: error("Unable to find `build-info.json` in JAR resources")

        val data = Json.decodeFromStream<JsonObject>(stream)
        isNightlyBuild = data["is_nightly_build"]?.jsonPrimitive?.booleanOrNull ?: error("`is_nightly_build` is missing or was not a boolean")
        commitHash = data["commit_hash"]?.jsonPrimitive?.contentOrNull ?: error("`commit_hash` is missing or was not a String")
        buildDate = data["build_date"]?.jsonPrimitive?.contentOrNull?.ifNotNull {
            Instant.parse(this)
        } ?: error("`build_date` is missing or was not a String")

        version = data["version"]?.jsonPrimitive?.contentOrNull ?: error("`version` is missing or was not a String")
    }

    /**
     * Represents the distribution info for this Hazel installation
     */
    enum class Distribution(private val possibleValues: List<String> = listOf()) {
        /**
         * Determines that this [Distribution] is running on a Kubernetes cluster. This can be either from
         * Noel on Cloud -- Kubernetes Operator, the Bamboo server Helm chart, or manually written.
         */
        KUBERNETES(listOf("kubernetes", "k8s")),

        /**
         * We couldn't find anything about this distribution. This might not be a legitimate copy,
         * be cautious!
         */
        UNKNOWN,

        /**
         * Determines that this [Distribution] is running from a Docker container.
         */
        DOCKER(listOf("docker")),

        /**
         * Determines that this [Distribution] was installed from Noelware's Debian distribution of Hazel
         * or from Noelware's Artifacts Registry.
         */
        DEB(listOf("deb")),

        /**
         * Determines that this [Distribution] was installed from Noelware's RPM distribution of Hazel
         * or from Noelware's Artifacts Registry.
         */
        RPM(listOf("rpm")),

        /**
         * Determines that this [Distribution] is running from the [GitHub repository](https://github.com/Noelware/hazel),
         * which might be a nightly build.
         */
        GIT(listOf("local", "git"));

        companion object {
            /**
             * Determines the [Distribution] from the system property: `org.noelware.hazel.distribution.type`
             */
            fun detect(): Distribution {
                val systemProp = System.getProperty("org.noelware.hazel.distribution.type")
                if (systemProp != null) return values().find { it.possibleValues.contains(systemProp) } ?: UNKNOWN

                return UNKNOWN
            }
        }
    }
}

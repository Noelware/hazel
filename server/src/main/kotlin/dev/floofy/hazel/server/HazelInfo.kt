package dev.floofy.hazel.server

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalSerializationApi::class)
object HazelInfo {
    /**
     * The version of **hazel** that was built, this is from the `build-info.json` file that
     * was generated when building the server.
     */
    val version: String

    /**
     * Represents the commit hash that was used to built the server from the `build-info.json` file
     * that was generated when building the server.
     */
    val commitHash: String

    /**
     * Represents the build date in ISO-8601 of when the server was built.
     */
    val buildDate: String

    /**
     * Returns the dedicated node the server is running off.
     */
    val dediNode by lazy {
        // Check if we have `winterfox.dediNode` in the Java properties
        val dediNode1 = System.getProperty("winterfox.dediNode", "")
        if (dediNode1.isNotEmpty()) {
            return@lazy dediNode1
        }

        // Maybe we only have the `WINTERFOX_DEDI_NODE` environment variable?
        // If we do, we'll assume that it is the dedi node name!
        val dediNode2 = System.getenv("WINTERFOX_DEDI_NODE")
        if (dediNode2 != null) {
            return@lazy dediNode2
        }

        // We can't find anything :(
        null
    }

    /**
     * Returns the [distribution type][DistributionType] that was used to run the server.
     */
    val distributionType = DistributionType.fromString(System.getProperty("dev.floofy.hazel.distribution.type", "?"))

    init {
        val stream = this::class.java.getResourceAsStream("/build-info.json")!!
        val data = Json.decodeFromStream<JsonObject>(stream)

        version = data["version"]?.jsonPrimitive?.content ?: error("Unable to retrieve `version` from build-info.json!")
        commitHash = data["commit_sha"]?.jsonPrimitive?.content ?: error("Unable to retrieve `commit.sha` from build-info.json!")
        buildDate = data["build_date"]?.jsonPrimitive?.content ?: error("Unable to retrieve `build.date` from build-info.json!")
    }
}

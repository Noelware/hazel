package dev.floofy.hazel.server.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Configuration for the Ktor server. This can customize the configuration
 * of how Netty is performed (since helm-server uses the Ktor + Netty engine!)
 */
@Serializable
data class KtorServerConfig(
    /**
     * If we should add additional security headers to the response.
     */
    @SerialName("security_headers")
    val securityHeaders: Boolean = true,

    /**
     * Size of the queue to store all the application call instances
     * that cannot be immediately processed.
     */
    @SerialName("request_queue_limit")
    val requestQueueLimit: Long = 16,

    /**
     * Number of concurrently running requests from the same HTTP pipeline
     */
    @SerialName("running_limit")
    val runningLimit: Long = 10,

    /**
     * Do not create separate call event groups and reuse worker
     * groups for processing calls.
     */
    @SerialName("share_work_group")
    val shareWorkGroup: Boolean = false,

    /**
     * Timeout in seconds for sending responses to the client.
     */
    @SerialName("response_write_timeout")
    val responseWriteTimeoutSeconds: Long = 10,

    /**
     * Timeout in seconds to read incoming requests from the client, "0" = infinite.
     */
    @SerialName("request_read_timeout")
    val requestReadTimeout: Long = 0,

    /**
     * If this is set to `true`, this will enable TCP keep alive for
     * connections that are so-called "dead" and can be easily discarded.
     *
     * The timeout period is configured by the system, so configure
     * the end host accordingly.
     */
    @SerialName("keep_alive")
    val tcpKeepAlive: Boolean = false,

    /**
     * Append extra headers when sending out a response.
     */
    @SerialName("extra_headers")
    val extraHeaders: Map<String, String> = mapOf(),

    /**
     * The host to bind to.
     */
    val host: String = "0.0.0.0",

    /**
     * The port to bind to.
     */
    val port: Long = 4949
)


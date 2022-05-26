package dev.floofy.hazel.server.config

import dev.floofy.hazel.server.serializers.DurationSerializer
import kotlinx.serialization.SerialName
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@kotlinx.serialization.Serializable
data class Config(
    @SerialName("sentry_dsn")
    val sentryDsn: String? = null,

    @kotlinx.serialization.Serializable(with = DurationSerializer::class)
    val invalidateRoutes: Duration = 30.milliseconds,

    val frontend: Boolean = false,
    val keystore: KeystoreConfig = KeystoreConfig("./data/keystore.jks"),
    val storage: StorageConfig,
    val server: KtorServerConfig = KtorServerConfig(),

    @SerialName("base_url")
    val baseUrl: String = "http://${server.host}:${server.port}",
)

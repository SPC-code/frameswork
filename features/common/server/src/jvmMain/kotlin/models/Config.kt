package space.kscience.frameswork.features.common.server.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * General server configuration for persistence, public addressing, and static content.
 *
 * The embedded Netty listener uses [KtorConfig]; [host] and [port] remain available to consumers of
 * this general configuration model.
 *
 * @property host configured application host.
 * @property port configured application port.
 * @property databaseConfig PostgreSQL/Exposed connection settings.
 * @property wss whether public WebSocket addresses should use the secure scheme.
 * @property publicHost host exposed to clients, defaulting to [host].
 * @property staticFolder optional shorthand for exposing one folder at the routing root.
 * @property staticFolders relative route prefixes mapped to local directories. When omitted, it is
 * derived from [staticFolder].
 */
@Serializable
data class Config(
    val host: String = "0.0.0.0",
    val port: Int = 8082,
    @SerialName("database")
    val databaseConfig: DatabaseConfig = DatabaseConfig(),
    val wss: Boolean = false,
    val publicHost: String = host,
    val staticFolder: String? = null,
    val staticFolders: Map<String, String> = staticFolder ?.let { mapOf("/" to it) } ?: emptyMap(),
)

package space.kscience.frameswork.features.common.server.models

import kotlinx.serialization.Serializable

/**
 * Network and routing settings used to construct the embedded Ktor server.
 *
 * @property host interface or host name on which Netty listens.
 * @property port TCP port on which Netty listens.
 * @property wss secure-WebSocket flag retained in configuration; this module does not interpret it.
 * @property rootRoute optional relative prefix applied to contributed application routes.
 */
@Serializable
data class KtorConfig(
    val host: String = "0.0.0.0",
    val port: Int = 8083,
    val wss: Boolean = false,
    val rootRoute: String? = null
)

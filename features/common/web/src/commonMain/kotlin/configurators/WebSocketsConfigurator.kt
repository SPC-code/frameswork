package space.kscience.frameswork.features.common.web.configurators

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import kotlinx.serialization.json.Json

/**
 * Enables Ktor WebSockets and configures serialized frames to use Kotlin serialization JSON.
 *
 * @property json serialization policy used by the WebSocket content converter.
 */
class WebSocketsConfigurator(
    private val json: Json
) : HttpClientConfigurator {
    /** Installs the Ktor WebSockets plugin and its JSON content converter. */
    override fun HttpClientConfig<*>.configure() {
        install(WebSockets.Plugin) {
            extensions {
                contentConverter = KotlinxWebsocketSerializationConverter(json)
            }
        }
    }
}

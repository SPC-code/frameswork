package space.kscience.frameswork.features.common.server.configurators

import dev.inmo.micro_utils.ktor.server.configurators.KtorApplicationConfigurator
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.websocket.WebSockets
import kotlinx.serialization.json.Json

/**
 * Installs Ktor WebSockets with kotlinx.serialization JSON frame conversion.
 *
 * This configurator enables protocol support but does not declare a WebSocket endpoint.
 *
 * @param json format used by the WebSocket content converter.
 */
class WebSocketsConfiguration(
    private val json: Json
) : KtorApplicationConfigurator {
    /** Installs [WebSockets] and its JSON serialization converter. */
    override fun Application.configure() {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(json)
        }
    }
}

package space.kscience.frameswork.features.common.web.configurators

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import kotlinx.serialization.json.Json

class WebSocketsConfigurator(
    private val json: Json
) : HttpClientConfigurator {
    override fun HttpClientConfig<*>.configure() {
        install(WebSockets.Plugin) {
            extensions {
                contentConverter = KotlinxWebsocketSerializationConverter(json)
            }
        }
    }
}
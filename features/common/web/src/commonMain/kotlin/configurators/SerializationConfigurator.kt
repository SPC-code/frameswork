package space.kscience.frameswork.features.common.web.configurators

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Enables JSON content negotiation and uses JSON as the default request content type.
 *
 * @property json serialization policy shared with the rest of the application.
 */
class SerializationConfigurator(private val json: Json) : HttpClientConfigurator {
    /** Installs Ktor's content-negotiation plugin with the supplied [json] instance. */
    override fun HttpClientConfig<*>.configure() {
        install(ContentNegotiation) {
            json(json)
        }
        defaultRequest {
            contentType(ContentType.Application.Json)
        }
    }
}

package space.kscience.frameswork.features.common.web.configurators

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

interface HttpClientConfigurator {
    fun HttpClientConfig<*>.configure()
}
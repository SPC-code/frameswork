package space.kscience.frameswork.features.common.web.configurators

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

/** Contributes one reusable configuration step to the module's shared Ktor HTTP client. */
interface HttpClientConfigurator {
    /** Applies this contribution to the [HttpClientConfig] currently being assembled. */
    fun HttpClientConfig<*>.configure()
}

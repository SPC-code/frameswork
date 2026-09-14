package space.kscience.frameswork.features.common.web.configurators

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.URLBuilder
import io.ktor.http.URLProtocol
import space.kscience.frameswork.features.common.web.utils.appendOrSetPartsWith
import space.kscience.frameswork.features.common.web.utils.fillAbsentPartsWith
import space.kscience.frameswork.features.common.web.utils.set

/**
 * Resolves relative Ktor client requests against a runtime-provided base URL.
 *
 * The request path is appended to the base path, request and base query parameters are retained,
 * and an absent base scheme is treated as HTTP. WebSocket requests are upgraded to `wss` when the
 * base URL is HTTPS and downgraded to `ws` when it is HTTP.
 *
 * @property urlGetter supplies the base URL immediately before each request.
 * @property useDefaultUrlPrefix whether to parse and normalize empty base-path segments before the
 * request URL is merged. This does not add a fixed endpoint prefix.
 */
class DefaultUrlHttpClientConfigurator(
    private val urlGetter: suspend () -> String,
    private val useDefaultUrlPrefix: Boolean = true
) : HttpClientConfigurator {
    /** Installs the request hook that merges every outgoing URL with the current base URL. */
    override fun HttpClientConfig<*>.configure() {
        val plugin = createClientPlugin("DefaultServerUrlPlugin") {
            onRequest { request, _ ->
                val currentUrl = urlGetter() ?: return@onRequest
                val requestProtocol = request.url.protocolOrNull
                val requestBuilder = request.url
                val schemeFixedUrl = when {
                    requestProtocol ?.name == "ws" && requestProtocol.defaultPort == 80 && currentUrl.startsWith("https://") -> {
                        requestBuilder.protocolOrNull = URLProtocol("wss", 443)
                        currentUrl
                    }
                    requestProtocol ?.name == "wss" && requestProtocol.defaultPort == 443 && currentUrl.startsWith("http://") -> {
                        requestBuilder.protocolOrNull = URLProtocol("ws", 80)
                        currentUrl
                    }
                    currentUrl.contains("://") -> {
                        currentUrl
                    }
                    else -> {
                        "http://$currentUrl"
                    }
                }
                // Ensure the base URL carries `/api` as the FIRST path segment (the server mounts every
                // route under `route("api")` at the routing root). Parse the URL first so an existing
                // path / query / fragment is preserved rather than matched by a fragile string suffix;
                // skip when disabled or when `/api` is already the leading segment.
                val fixedCurrentUrl = when {
                    useDefaultUrlPrefix.not() -> schemeFixedUrl
                    else -> {
                        val builder = URLBuilder(schemeFixedUrl)
                        val pathSegments = builder.encodedPathSegments.filter { it.isNotEmpty() }
                        builder.encodedPathSegments = pathSegments
                        builder.buildString()
                    }
                }
                val newUrlBuilder = URLBuilder(fixedCurrentUrl)
                val initialUrl = request.url
                newUrlBuilder.appendOrSetPartsWith(fallbackValues = initialUrl, forceSetDefaultProtocol = true)
                request.url.set(newUrlBuilder)
            }
        }
        install(plugin)
    }
}

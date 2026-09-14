package space.kscience.frameswork.features.common.server.configurators

import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.micro_utils.ktor.server.configurators.KtorApplicationConfigurator
import io.ktor.http.invoke
import io.ktor.server.application.Application
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * Installs contributed routing elements, optionally beneath a common relative route prefix.
 *
 * The configurator does not define endpoints of its own; each [ApplicationRoutingConfigurator.Element]
 * supplies its routes.
 *
 * @param elements route contributions to install in order.
 * @param rootPath optional route prefix under which every contribution is installed.
 */
@Serializable
class InternalApplicationRoutingConfigurator(
    private val elements: List<@Contextual ApplicationRoutingConfigurator.Element>,
    private val rootPath: String? = null,
) : KtorApplicationConfigurator {
    /** Combines all [elements] into one route contribution. */
    private val rootInstaller = ApplicationRoutingConfigurator.Element {
        elements.forEach {
            it.apply { invoke() }
        }
    }

    /** Creates Ktor routing and invokes every contribution at [rootPath], or at the routing root. */
    override fun Application.configure() {
        routing {
            this@InternalApplicationRoutingConfigurator.rootPath ?.let {
                route(it) {
                    with(rootInstaller) {
                        this@route.invoke()
                    }
                }
                return@routing
            }
            rootInstaller.apply { invoke() }
        }
    }
}

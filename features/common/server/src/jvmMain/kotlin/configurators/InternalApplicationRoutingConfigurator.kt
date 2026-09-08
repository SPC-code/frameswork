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


@Serializable
class InternalApplicationRoutingConfigurator(
    private val elements: List<@Contextual ApplicationRoutingConfigurator.Element>,
    private val rootPath: String? = null,
) : KtorApplicationConfigurator {
    private val rootInstaller = ApplicationRoutingConfigurator.Element {
        elements.forEach {
            it.apply { invoke() }
        }
    }

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

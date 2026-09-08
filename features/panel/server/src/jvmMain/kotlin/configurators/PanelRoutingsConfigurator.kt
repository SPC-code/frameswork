package space.kscience.frameswork.features.panel.server.configurators

import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import space.kscience.frameswork.features.panel.common.PanelConstants
import space.kscience.frameswork.features.panel.common.PanelFeature
import space.kscience.frameswork.features.panel.common.models.PanelInfo

class PanelRoutingsConfigurator(
    private val panelFeature: PanelFeature
) : ApplicationRoutingConfigurator.Element {
    override fun Route.invoke() {
        route(PanelConstants.rootPanelPath) {
            get(PanelConstants.getPanelSubpath) {
                val config = panelFeature.getPanelConfig()
                if (config == null) {
                    call.respond("null")
                } else {
                    call.respond(config)
                }
            }
            post(PanelConstants.setPanelSubpath) {
                val config = call.receive<PanelInfo>()
                val setResult = panelFeature.setPanelConfig(config)
                call.respond(setResult)
            }
        }
    }
}
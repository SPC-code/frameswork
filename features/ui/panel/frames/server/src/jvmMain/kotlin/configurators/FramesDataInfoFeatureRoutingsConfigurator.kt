package space.kscience.frameswork.features.ui.panel.frames.server.configurators

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.micro_utils.coroutines.runCatchingLogging
import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.micro_utils.repos.ktor.common.idParameterName
import io.ktor.server.response.respond
import io.ktor.server.response.respondNullable
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.util.getOrFail
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import space.kscience.frameswork.features.frames.common.utils.encodeToByteArray
import space.kscience.frameswork.features.ui.panel.frames.common.PanelCameraConstants
import space.kscience.frameswork.features.ui.panel.frames.common.features.FramesFlowFeatureId
import space.kscience.frameswork.features.ui.panel.frames.common.features.FramesDataInfoFeature

class FramesDataInfoFeatureRoutingsConfigurator(
    private val feature: FramesDataInfoFeature,
    private val json: Json
) : ApplicationRoutingConfigurator.Element {
    override fun Route.invoke() {
        route(
            PanelCameraConstants.rootPathPart
        ) {
            get(PanelCameraConstants.getAvailableProcessorsPathPart) {
                call.respond(feature.getAvailableProcessors())
            }
            get(PanelCameraConstants.getAvailableCamerasPathPart) {
                val processorName = call.parameters.getOrFail<String>(idParameterName)
                call.respondNullable(feature.getAvailableFramesSources(processorName))
            }

            webSocket(PanelCameraConstants.getFramesPathPart) {
                val framesFlowFeatureIdState = MutableRedeliverStateFlow<FramesFlowFeatureId?>(null)

                launchLoggingDropExceptions {
                    while (isActive && incoming.isClosedForReceive == false) {
                        val it = incoming.receive()
                        when (it) {
                            is Frame.Binary -> {}
                            is Frame.Close -> {}
                            is Frame.Ping -> {}
                            is Frame.Pong -> {}
                            is Frame.Text -> {
                                val text = it.readText()
                                if (text == "ping") {
                                    outgoing.send(Frame.Text("pong"))
                                } else {
                                    framesFlowFeatureIdState.value = runCatchingLogging {
                                        json.decodeFromString(FramesFlowFeatureId.serializer(), it.readText())
                                    }.getOrNull() ?: framesFlowFeatureIdState.value
                                }
                            }
                        }
                    }
                }
                framesFlowFeatureIdState
                    .flatMapLatest {
                        val it = it ?: return@flatMapLatest emptyFlow()
                        feature.getFramesFlow(it.processorName, it.framesSourceId)
                    }
                    .collect {
                        outgoing.send(
                            Frame.Binary(
                                fin = true,
                                data = it.encodeToByteArray(json)
                            )
                        )
                    }
            }
        }
    }
}

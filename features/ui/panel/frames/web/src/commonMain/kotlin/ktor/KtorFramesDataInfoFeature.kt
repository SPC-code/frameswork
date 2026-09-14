package space.kscience.frameswork.features.ui.panel.frames.ktor

import dev.inmo.micro_utils.coroutines.runCatchingLogging
import dev.inmo.micro_utils.ktor.client.bodyOrNull
import dev.inmo.micro_utils.repos.ktor.common.idParameterName
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.ws
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.websocket.Frame
import korlibs.time.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import space.kscience.frameswork.features.common.common.utils.toSharedFlow
import space.kscience.frameswork.features.frames.common.models.ByteArrayFrameData
import space.kscience.frameswork.features.frames.common.models.FramesSourceId
import space.kscience.frameswork.features.frames.common.utils.decodeFrameData
import space.kscience.frameswork.features.ui.panel.frames.common.PanelCameraConstants
import space.kscience.frameswork.features.ui.panel.frames.common.features.FramesFlowFeatureId
import space.kscience.frameswork.features.ui.panel.frames.common.features.FramesDataInfoFeature
import kotlin.time.Duration

/**
 * Remote [FramesDataInfoFeature] that obtains metadata over HTTP and frames over WebSocket.
 *
 * Frame flows are shared per processor/source pair. A shared flow reconnects after a connection
 * ends and uses an application-level `ping`/`pong` exchange to detect an inactive connection.
 *
 * @param client HTTP client used for metadata requests and WebSocket sessions.
 * @param json format used for frame-subscription messages and binary frame metadata.
 * @param receiveFrameTimeoutMillis maximum wait for the next frame before sending a text `ping`.
 * @param receivePongTimeoutMillis maximum wait after `ping` before closing an inactive session.
 * @param reconnectTimeoutMillis delay before another WebSocket connection attempt.
 * @param scope scope in which processor/source frame flows are shared.
 */
open class KtorFramesDataInfoFeature(
    private val client: HttpClient,
    private val json: Json,
    private val receiveFrameTimeoutMillis: Duration = 1.seconds,
    private val receivePongTimeoutMillis: Duration = 5.seconds,
    private val reconnectTimeoutMillis: Duration = 3.seconds,
    private val scope: CoroutineScope,
) : FramesDataInfoFeature {
    private val getAvailableProcessorsFullPath = "${PanelCameraConstants.rootPathPart}/${PanelCameraConstants.getAvailableProcessorsPathPart}"
    private val getAvailableCamerasFullPath = "${PanelCameraConstants.rootPathPart}/${PanelCameraConstants.getAvailableCamerasPathPart}"
    private val getFramesFlowFullPath = "${PanelCameraConstants.rootPathPart}/${PanelCameraConstants.getFramesPathPart}"

    /**
     * Requests the available processor names, falling back to an empty set when the response status
     * is not `200 OK`.
     */
    override suspend fun getAvailableProcessors(): Set<String> {
        return client.get(getAvailableProcessorsFullPath).bodyOrNull() ?: emptySet()
    }

    /**
     * Requests frame sources for [processorName]. A nullable result is preserved so callers can
     * distinguish an unavailable result from a processor with no sources.
     */
    override suspend fun getAvailableFramesSources(processorName: String): Set<FramesSourceId>? {
        return client.get(getAvailableCamerasFullPath) {
            parameter(idParameterName, processorName)
        }.bodyOrNull()
    }

    private suspend fun DefaultClientWebSocketSession.handleFramesWebsocket(
        processorName: String,
        id: FramesSourceId,
        returningFlow: ProducerScope<ByteArrayFrameData>
    ) {
        val ws = this
        outgoing.send(
            Frame.Text(
                json.encodeToString(
                    FramesFlowFeatureId.serializer(),
                    FramesFlowFeatureId(processorName, id)
                )
            )
        )
        while (incoming.isClosedForReceive == false && returningFlow.isActive) {
            val sourceFrame = withTimeoutOrNull(receiveFrameTimeoutMillis) {
                runCatchingLogging {
                    incoming.receive()
                }.getOrNull()
            }
            val wsFrame = if (sourceFrame == null) {
                withTimeoutOrNull(receivePongTimeoutMillis) {
                    runCatchingLogging {
                        outgoing.send(Frame.Text("ping"))
                        incoming.receive()
                    }.getOrNull()
                }
            } else {
                sourceFrame
            }
            runCatchingLogging {
                when (wsFrame) {
                    is Frame.Binary -> {
                        val frameData = wsFrame.data.decodeFrameData(json)
                        returningFlow.send(
                            frameData
                        )
                    }
                    is Frame.Ping -> {}
                    is Frame.Pong -> {}
                    is Frame.Text -> {}
                    null,
                    is Frame.Close -> {
                        ws.cancel()
                        break
                    }
                }
            }
        }
    }

    /**
     * Returns the shared, reconnecting WebSocket frame flow for [processorName] and [id].
     *
     * Repeated calls for the same pair reuse the previously allocated flow.
     */
    override fun getFramesFlow(processorName: String, id: FramesSourceId): Flow<ByteArrayFrameData> {
        return channelFlow {
            val returningFlow = this

            while (returningFlow.isActive) {
                runCatchingLogging {
                    client.ws(
                        getFramesFlowFullPath,
                    ) {
                        handleFramesWebsocket(processorName, id, returningFlow)
                    }
                }
                delay(reconnectTimeoutMillis)
            }
        }.toSharedFlow(scope = scope).first
    }
}

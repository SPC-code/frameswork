package space.kscience.frameswork.features.frames.common.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import space.kscience.frameswork.features.frames.common.services.FrameSourceConnector
import space.kscience.frameswork.features.frames.common.services.connectors.FFMPEGRTSPFrameSourceConnector

@Serializable
@SerialName("ffmpeg_rtsp")
data class FFMPEGRTSPConfig(
    val id: String,
    val url: String
) : FrameSourceConnectorConfig {
    override fun createFramesSourceConnector(): FrameSourceConnector {
        return FFMPEGRTSPFrameSourceConnector(
            FramesSourceId(id),
            url
        )
    }
}
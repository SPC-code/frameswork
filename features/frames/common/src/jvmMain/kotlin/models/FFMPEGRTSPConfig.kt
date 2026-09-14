package space.kscience.frameswork.features.frames.common.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import space.kscience.frameswork.features.frames.common.services.FrameSourceConnector
import space.kscience.frameswork.features.frames.common.services.connectors.FFMPEGRTSPFrameSourceConnector

/**
 * Serializable configuration for an FFmpeg-backed RTSP frame source.
 *
 * @property id textual identifier assigned to the created connector.
 * @property url RTSP URL opened by the connector.
 */
@Serializable
@SerialName("ffmpeg_rtsp")
data class FFMPEGRTSPConfig(
    val id: String,
    val url: String
) : FrameSourceConnectorConfig {
    /** Creates a new [FFMPEGRTSPFrameSourceConnector] from [id] and [url]. */
    override fun createFramesSourceConnector(): FrameSourceConnector {
        return FFMPEGRTSPFrameSourceConnector(
            FramesSourceId(id),
            url
        )
    }
}

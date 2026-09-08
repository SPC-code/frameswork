package space.kscience.frameswork.features.frames.common.services

import space.kscience.frameswork.features.frames.common.models.FrameSourceConnectorConfig
import space.kscience.frameswork.features.frames.common.models.FramesSourceId
import space.kscience.frameswork.features.frames.common.models.FramesSource

interface FrameSourceConnector : FramesSource {
    val id: FramesSourceId
    fun createConfig(): FrameSourceConnectorConfig
}

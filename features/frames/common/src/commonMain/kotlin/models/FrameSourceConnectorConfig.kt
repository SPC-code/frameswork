package space.kscience.frameswork.features.frames.common.models

import space.kscience.frameswork.features.frames.common.services.FrameSourceConnector

interface FrameSourceConnectorConfig {
    fun createFramesSourceConnector(): FrameSourceConnector
}
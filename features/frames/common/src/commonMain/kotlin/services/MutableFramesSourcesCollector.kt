package space.kscience.frameswork.features.frames.common.services

import space.kscience.frameswork.features.frames.common.models.FramesSourceId

interface MutableFramesSourcesCollector : FramesSourcesCollector {
    suspend fun addCamera(frameSourceConnector: FrameSourceConnector): Boolean
    suspend fun removeCamera(id: FramesSourceId): Boolean
}
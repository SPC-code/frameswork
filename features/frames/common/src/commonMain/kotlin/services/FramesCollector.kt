package space.kscience.frameswork.features.frames.common.services

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import space.kscience.frameswork.features.frames.common.models.FramesSourceId
import space.kscience.frameswork.features.frames.common.models.FrameData

/**
 * Aggregate frames [Flow]s to not connect to cameras several times
 */
interface FramesCollector {
    val sourcesListUpdatesFlow: StateFlow<Set<FramesSourceId>>

    fun allocateFramesFlow(id: FramesSourceId): Flow<FrameData>?
}

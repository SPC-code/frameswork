package space.kscience.frameswork.features.frames.common.services

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import space.kscience.frameswork.features.frames.common.models.FramesSourceId
import space.kscience.frameswork.features.frames.common.models.FramesSource

interface FramesSourcesCollector {
    val framesSourcesIdsListUpdatesFlow: StateFlow<Set<FramesSourceId>>

    suspend fun getAvailableFramesSourcesIds(): Set<FramesSourceId>

    fun allocateConnectorFlow(id: FramesSourceId): StateFlow<FramesSource?>
}

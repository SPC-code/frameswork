package space.kscience.frameswork.features.frames.common.services

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import space.kscience.frameswork.features.frames.common.models.FramesSourceId
import space.kscience.frameswork.features.frames.common.models.FrameData

/** Aggregates frame [Flow]s so multiple consumers can share source connections. */
interface FramesCollector {
    /** Current identifiers for which frame flows can be allocated. */
    val sourcesListUpdatesFlow: StateFlow<Set<FramesSourceId>>

    /**
     * Obtains the aggregated frame flow for [id].
     *
     * @param id source identifier to look up.
     * @return the source flow, or `null` when the source is unavailable.
     */
    fun allocateFramesFlow(id: FramesSourceId): Flow<FrameData>?
}

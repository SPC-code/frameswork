package space.kscience.frameswork.features.frames.common.services

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import space.kscience.frameswork.features.frames.common.models.FramesSourceId
import space.kscience.frameswork.features.frames.common.models.FramesSource

/** Read-only registry of frame sources and their live connector updates. */
interface FramesSourcesCollector {
    /** Current set of available source identifiers. */
    val framesSourcesIdsListUpdatesFlow: StateFlow<Set<FramesSourceId>>

    /**
     * Reads all currently available source identifiers.
     *
     * @return a snapshot of registered source identifiers.
     */
    suspend fun getAvailableFramesSourcesIds(): Set<FramesSourceId>

    /**
     * Observes the source registered under [id].
     *
     * @param id identifier to observe.
     * @return a state flow whose value is `null` while the source is unavailable.
     */
    fun allocateConnectorFlow(id: FramesSourceId): StateFlow<FramesSource?>
}

package space.kscience.frameswork.features.frames.common.services

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import space.kscience.frameswork.features.frames.common.models.FramesSourceId
import space.kscience.frameswork.features.frames.common.models.FrameData
import space.kscience.frameswork.features.common.common.utils.halfColdFlows
import space.kscience.frameswork.features.frames.common.models.OneConnectionFramesSource

/**
 * Default [FramesCollector] that shares one upstream flow for each available source.
 *
 * Source additions, removals, and connector replacements are followed through [framesCollector]. A
 * source flow is collected only while it has subscribers, preventing duplicate camera connections.
 *
 * @param framesCollector registry used to discover sources and observe their connectors.
 * @param scope lifecycle scope for the shared flows and [sourcesListUpdatesFlow].
 */
class DefaultFramesCollector(
    private val framesCollector: FramesSourcesCollector,
    scope: CoroutineScope
) : FramesCollector {
    private val persistentCamerasConnectorsFlows: Flow<Map<FramesSourceId, Flow<FrameData>>> = framesCollector.framesSourcesIdsListUpdatesFlow.map {
        framesCollector.getAvailableFramesSourcesIds().associateWith {
            framesCollector.allocateConnectorFlow(it).flatMapLatest {
                it ?.allocateFlow() ?: emptyFlow()
            }
        }
    }
    private val persistentFramesFlowAndJob: Pair<StateFlow<Map<FramesSourceId, Flow<FrameData>>>, Job> = persistentCamerasConnectorsFlows
        .halfColdFlows(scope)
    private val persistentFramesFlow = persistentFramesFlowAndJob.first
    private val persistentFramesFlowJob = persistentFramesFlowAndJob.second
    override val sourcesListUpdatesFlow: StateFlow<Set<FramesSourceId>> = persistentFramesFlow
        .map { it.keys }
        .stateIn(scope = scope, SharingStarted.Eagerly, initialValue = emptySet())

    /**
     * Returns the shared flow currently allocated for [id].
     *
     * @param id identifier of an available frame source.
     * @return the source's shared flow, or `null` when [id] is not currently available.
     */
    override fun allocateFramesFlow(id: FramesSourceId): Flow<FrameData>? {
        return persistentFramesFlow.value[id]
    }
}

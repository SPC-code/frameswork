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
 * Default implementation of the [FramesCollector] interface, designed to aggregate and manage
 * streams of frame data from multiple frame sources. It prevents multiple connections to the
 * same camera by managing a centralized flow of frame sources and their respective frame data streams.
 *
 * Internally, this class interacts with a [FramesSourcesCollector], which provides utilities
 * to enumerate available frame sources and their connections. It uses a "half-cold" flow mechanism
 * for efficient subscription management, ensuring that frame data streams are active only when required
 * by subscribers.
 *
 * @constructor
 * @param framesCollector A [FramesSourcesCollector] instance responsible for discovering and
 * managing underlying frame sources and their connections.
 * @param scope The [CoroutineScope] in which internal coroutines and flows are executed. This scope
 * is used to ensure proper lifecycle management of processing jobs and flows.
 *
 * @property sourcesListUpdatesFlow A [StateFlow] that emits updates to the set of currently available
 * frame sources, represented by their [FramesSourceId]. The set reflects the keys of the frame sources
 * managed by this collector.
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

    override fun allocateFramesFlow(id: FramesSourceId): Flow<FrameData>? {
        return persistentFramesFlow.value[id]
    }
}

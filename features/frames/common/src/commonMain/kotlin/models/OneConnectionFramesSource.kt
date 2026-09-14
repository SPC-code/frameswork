package space.kscience.frameswork.features.frames.common.models

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.mapLatest
import space.kscience.frameswork.features.common.common.utils.toHalfColdFlow

/**
 * Shares at most one active upstream frame-source connection among all downstream subscribers.
 *
 * When [baseFramesSources] emits a different source, existing subscribers are switched to the new
 * source. A `null` source produces an empty stream. The shared connection is managed in [scope].
 *
 * @param baseFramesSources updates of the source whose frames should be exposed.
 * @param scope lifecycle scope for the shared upstream collection.
 */
class OneConnectionFramesSource(
    baseFramesSources: Flow<FramesSource?>,
    scope: CoroutineScope,
) : FramesSource {
    private val persistentCamerasConnectorsFlows = baseFramesSources.mapLatest {
        it ?.allocateFlow() ?: emptyFlow()
    }
    private val persistentFramesFlowAndJob = persistentCamerasConnectorsFlows.toHalfColdFlow(scope)
    private val persistentFramesFlow = persistentFramesFlowAndJob.first
    private val persistentFramesFlowJob = persistentFramesFlowAndJob.second

    /** Returns the shared frame flow managed by this source. */
    override fun allocateFlow(): Flow<FrameData> {
        return persistentFramesFlow
    }
}

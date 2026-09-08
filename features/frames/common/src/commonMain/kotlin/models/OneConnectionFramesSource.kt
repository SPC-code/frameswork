package space.kscience.frameswork.features.frames.common.models

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.mapLatest
import space.kscience.frameswork.features.common.common.utils.toHalfColdFlow

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

    override fun allocateFlow(): Flow<FrameData> {
        return persistentFramesFlow
    }
}
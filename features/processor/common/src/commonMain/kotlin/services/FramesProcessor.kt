package space.kscience.frameswork.features.processor.common.services

import kotlinx.coroutines.flow.Flow
import space.kscience.frameswork.features.frames.common.models.FramesSourceId
import space.kscience.frameswork.features.frames.common.models.FrameData
import space.kscience.frameswork.features.frames.common.services.FramesCollector

/**
 * Processes frames both as a standalone [FramesProcessorMiddleware] and as a source-aware [FramesCollector].
 */
interface FramesProcessor : FramesCollector, FramesProcessorMiddleware {
    /**
     * Allocates a flow handle that remains valid while [id] is unavailable or replaced.
     *
     * Unlike [allocateFramesFlow], this function never returns `null`: the returned flow remains quiet while
     * the source is absent and follows the processed flow when the source becomes available again.
     *
     * @param id Source whose processed frames should be observed.
     * @return A flow that follows the currently available processed stream for [id].
     */
    fun allocatePersistentFramesFlow(id: FramesSourceId, ): Flow<FrameData>
}

package space.kscience.frameswork.features.processor.common.services

import kotlinx.coroutines.flow.Flow
import space.kscience.frameswork.features.frames.common.models.FramesSourceId
import space.kscience.frameswork.features.frames.common.models.FrameData
import space.kscience.frameswork.features.frames.common.services.FramesCollector

interface FramesProcessor : FramesCollector, FramesProcessorMiddleware {
    fun allocatePersistentFramesFlow(id: FramesSourceId, ): Flow<FrameData>
}

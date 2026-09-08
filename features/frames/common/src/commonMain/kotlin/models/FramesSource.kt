package space.kscience.frameswork.features.frames.common.models

import kotlinx.coroutines.flow.Flow

interface FramesSource {
    fun allocateFlow(): Flow<FrameData>
}
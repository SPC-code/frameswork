package space.kscience.frameswork.features.ui.panel.frames.ui.camera

import kotlinx.coroutines.flow.Flow
import space.kscience.frameswork.features.frames.common.models.ByteArrayFrameData
import space.kscience.frameswork.features.frames.common.models.FramesSourceId

interface CameraModel {
    suspend fun getAvailableProcessors(): Set<String>
    suspend fun getAvailableCameras(processorName: String): Set<FramesSourceId>?
    fun getCameraFrames(processorName: String, id: FramesSourceId): Flow<ByteArrayFrameData>
}
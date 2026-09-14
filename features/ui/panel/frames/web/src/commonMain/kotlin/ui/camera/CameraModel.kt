package space.kscience.frameswork.features.ui.panel.frames.ui.camera

import kotlinx.coroutines.flow.Flow
import space.kscience.frameswork.features.frames.common.models.ByteArrayFrameData
import space.kscience.frameswork.features.frames.common.models.FramesSourceId

/** Supplies processor discovery, camera discovery, and frame streams to the camera UI. */
interface CameraModel {
    /** Returns the currently available frame-processor names. */
    suspend fun getAvailableProcessors(): Set<String>

    /**
     * Returns the camera sources exposed by [processorName], or `null` when none can be resolved.
     */
    suspend fun getAvailableCameras(processorName: String): Set<FramesSourceId>?

    /** Returns frames produced by [processorName] for camera source [id]. */
    fun getCameraFrames(processorName: String, id: FramesSourceId): Flow<ByteArrayFrameData>
}

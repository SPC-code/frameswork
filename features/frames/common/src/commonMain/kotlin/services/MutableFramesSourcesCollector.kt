package space.kscience.frameswork.features.frames.common.services

import space.kscience.frameswork.features.frames.common.models.FramesSourceId

/** A [FramesSourcesCollector] whose connector registry can be changed at runtime. */
interface MutableFramesSourcesCollector : FramesSourcesCollector {
    /**
     * Adds or replaces [frameSourceConnector].
     *
     * @return `true` when the connector is present after the operation.
     */
    suspend fun addCamera(frameSourceConnector: FrameSourceConnector): Boolean

    /**
     * Removes the connector identified by [id].
     *
     * @return `true` when the connector is absent after the operation.
     */
    suspend fun removeCamera(id: FramesSourceId): Boolean
}

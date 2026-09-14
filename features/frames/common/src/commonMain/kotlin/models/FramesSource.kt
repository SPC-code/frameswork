package space.kscience.frameswork.features.frames.common.models

import kotlinx.coroutines.flow.Flow

/** A producer that can allocate streams of [FrameData]. */
interface FramesSource {
    /**
     * Allocates a frame stream for a subscriber.
     *
     * Whether separate calls share an upstream connection depends on the implementation.
     *
     * @return a flow that emits frames from this source.
     */
    fun allocateFlow(): Flow<FrameData>
}

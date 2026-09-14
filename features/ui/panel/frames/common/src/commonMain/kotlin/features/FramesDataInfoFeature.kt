package space.kscience.frameswork.features.ui.panel.frames.common.features

import kotlinx.coroutines.flow.Flow
import space.kscience.frameswork.features.frames.common.models.ByteArrayFrameData
import space.kscience.frameswork.features.frames.common.models.FramesSourceId

/**
 * Provides the camera panel with available processors, frame-source metadata, and frame streams.
 *
 * A frame stream is addressed by both a processor name and a [FramesSourceId]. Metadata queries
 * are independent of stream collection and return finite snapshots rather than update flows.
 */
interface FramesDataInfoFeature {
    /**
     * Returns the names of processors that are currently available for frame processing.
     */
    suspend fun getAvailableProcessors(): Set<String>

    /**
     * Returns the frame sources currently exposed by the processor named [processorName].
     *
     * @return the processor's frame-source identifiers, or `null` when the processor is unknown.
     */
    suspend fun getAvailableFramesSources(processorName: String): Set<FramesSourceId>?

    /**
     * Returns a stream of byte-backed frames produced for [id] by [processorName].
     *
     * Each emitted [ByteArrayFrameData] contains both the raw frame payload and its metadata. The
     * returned flow's sharing, reconnection, and unavailable-source behavior are implementation-specific.
     */
    fun getFramesFlow(processorName: String, id: FramesSourceId): Flow<ByteArrayFrameData>
}

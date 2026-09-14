package space.kscience.frameswork.features.ui.panel.frames.common.features

import kotlinx.serialization.Serializable
import space.kscience.frameswork.features.frames.common.models.FramesSourceId

/**
 * Serializable selection of one processor's frame source.
 *
 * Remote implementations can use this value as the request that selects which frames a stream
 * should emit.
 *
 * @property processorName name of the processor that owns the selected source.
 * @property framesSourceId identifier of the selected frame source.
 */
@Serializable
data class FramesFlowFeatureId(
    val processorName: String,
    val framesSourceId: FramesSourceId
)

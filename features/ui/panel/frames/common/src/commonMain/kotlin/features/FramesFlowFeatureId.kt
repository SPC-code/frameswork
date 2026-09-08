package space.kscience.frameswork.features.ui.panel.frames.common.features

import kotlinx.serialization.Serializable
import space.kscience.frameswork.features.frames.common.models.FramesSourceId

@Serializable
data class FramesFlowFeatureId(
    val processorName: String,
    val framesSourceId: FramesSourceId
)

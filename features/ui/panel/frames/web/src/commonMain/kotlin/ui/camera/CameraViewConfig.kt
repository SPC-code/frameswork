package space.kscience.frameswork.features.ui.panel.frames.ui.camera

import kotlinx.serialization.Serializable
import space.kscience.frameswork.features.common.web.models.ViewConfig
import space.kscience.frameswork.features.ui.panel.frames.common.features.FramesFlowFeatureId

@Serializable
data class CameraViewConfig(
    val framesFlowFeatureId: FramesFlowFeatureId
) : ViewConfig

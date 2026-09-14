package space.kscience.frameswork.features.ui.panel.frames.ui.camera

import kotlinx.serialization.Serializable
import space.kscience.frameswork.features.common.web.models.ViewConfig
import space.kscience.frameswork.features.ui.panel.frames.common.features.FramesFlowFeatureId

/**
 * Serializable navigation configuration for a camera stream view.
 *
 * @property framesFlowFeatureId processor and frame-source pair displayed by the view.
 */
@Serializable
data class CameraViewConfig(
    /** Processor and frame-source pair displayed by the view. */
    val framesFlowFeatureId: FramesFlowFeatureId
) : ViewConfig

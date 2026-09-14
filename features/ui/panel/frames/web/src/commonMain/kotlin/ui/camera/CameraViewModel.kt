package space.kscience.frameswork.features.ui.panel.frames.ui.camera

import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.mvvm.ViewModel
import kotlinx.coroutines.flow.flatMapLatest
import space.kscience.frameswork.features.common.web.models.ViewConfig

/**
 * Adapts camera navigation state to the frame stream consumed by the camera view.
 *
 * Whenever [node] receives a new [CameraViewConfig], [frames] cancels collection of the previous
 * stream and switches to the stream returned by [model] for the new selection.
 *
 * @param node navigation node whose configuration selects the displayed camera stream.
 * @param model camera data source used to resolve the selected stream.
 */
class CameraViewModel(
    private val node: NavigationNode<CameraViewConfig, ViewConfig>,
    private val model: CameraModel
) : ViewModel<ViewConfig>(node) {
    /** Frame flow for the processor and source in the latest navigation configuration. */
    val frames = node.configState.flatMapLatest {
        model.getCameraFrames(it.framesFlowFeatureId.processorName, it.framesFlowFeatureId.framesSourceId)
    }
}

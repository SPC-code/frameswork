package space.kscience.frameswork.features.ui.panel.frames.ui.camera

import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.mvvm.ViewModel
import kotlinx.coroutines.flow.flatMapLatest
import space.kscience.frameswork.features.common.web.models.ViewConfig

class CameraViewModel(
    private val node: NavigationNode<CameraViewConfig, ViewConfig>,
    private val model: CameraModel
) : ViewModel<ViewConfig>(node) {
    val frames = node.configState.flatMapLatest {
        model.getCameraFrames(it.framesFlowFeatureId.processorName, it.framesFlowFeatureId.framesSourceId)
    }
}
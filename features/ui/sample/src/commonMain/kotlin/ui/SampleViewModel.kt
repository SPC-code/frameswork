package space.kscience.frameswork.features.ui.sample.ui

import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.mvvm.ViewModel
import space.kscience.frameswork.features.common.web.models.ViewConfig

/**
 * View model for a [SampleViewConfig] navigation node.
 *
 * This sample currently delegates navigation lifecycle handling to [ViewModel] and keeps the
 * injected [SampleModel] available for future screen behaviour.
 *
 * @param node navigation node whose configuration and lifecycle are managed by this view model.
 * @param model model service associated with the sample screen.
 */
class SampleViewModel(
    /** Navigation node managed through the [ViewModel] base class. */
    private val node: NavigationNode<SampleViewConfig, ViewConfig>,
    /** Model service reserved for sample-screen state and operations. */
    private val model: SampleModel
) : ViewModel<ViewConfig>(node) {

}

package space.kscience.frameswork.features.ui.sample.ui

import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.mvvm.ViewModel
import space.kscience.frameswork.features.common.web.models.ViewConfig

class SampleViewModel(
    private val node: NavigationNode<SampleViewConfig, ViewConfig>,
    private val model: SampleModel
) : ViewModel<ViewConfig>(node) {

}
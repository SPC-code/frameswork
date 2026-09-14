package space.kscience.frameswork.features.ui.sample.ui

import androidx.compose.runtime.Composable
import dev.inmo.micro_utils.colors.common.HEXAColor
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import org.jetbrains.compose.web.css.CSSColorValue
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import org.koin.core.parameter.parametersOf
import space.kscience.frameswork.features.common.web.models.ViewConfig
import org.koin.core.component.inject
import space.kscience.frameswork.features.common.web.ui.FlotViewDashboard
import kotlin.js.unsafeCast

/**
 * Compose Web navigation view for [SampleViewConfig].
 *
 * The view obtains a fresh [SampleViewModel] from Koin with itself as the navigation-node
 * parameter. Its current content is the parameterless FlotView reference dashboard.
 *
 * @param chain navigation chain that owns this view.
 * @param config serializable configuration associated with this view instance.
 */
class SampleView(
    chain: NavigationChain<ViewConfig>,
    config: SampleViewConfig,
) : ComposeView<SampleViewConfig, ViewConfig, SampleViewModel>(config, chain) {
    /** Lazily injected view model bound to this navigation node. */
    override val viewModel: SampleViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) { parametersOf(this@SampleView) }

    /** Draws the shared FlotView reference dashboard. */
    @Composable
    override fun onDraw() {
        super.onDraw()
        FlotViewDashboard()
//        Div({
//            config.backgroundColor ?.let {
//                style {
//                    backgroundColor(it.rgb.unsafeCast<CSSColorValue>())
//                    width(100.percent)
//                    height(100.percent)
//                }
//            }
//        }) {
//            Text(config.text)
//        }
    }
}

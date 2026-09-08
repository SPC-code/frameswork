package space.kscience.frameswork.client

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import dev.inmo.micro_utils.common.either
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.compose.InjectNavigationChain
import dev.inmo.navigation.compose.InjectNavigationNode
import dev.inmo.navigation.compose.getChainFromLocalProvider
import dev.inmo.navigation.compose.initNavigation
import dev.inmo.navigation.compose.nodeFactory
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationChainId
import dev.inmo.navigation.core.NavigationNode
import space.kscience.frameswork.features.common.web.models.ViewConfig
import dev.inmo.navigation.core.extensions.changesInSubtreeFlow
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module
import space.kscience.frameswork.features.common.web.models.EmptyConfig
import space.kscience.frameswork.features.ui.panel.ui.PanelViewConfig
import space.kscience.frameswork.features.ui.sample.ui.SampleViewConfig

object ClientPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {

    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        koin.get<(@Composable () -> Unit) -> Unit>().invoke {
            initNavigation<ViewConfig>(
                EmptyConfig(),
                configsRepo = koin.get(),
                nodesFactory = koin.nodeFactory(),
                dropRedundantChainsOnRestore = true,
            ) {
                val rootChain = getChainFromLocalProvider<ViewConfig>()!!
                LaunchedEffect(rootChain) {
                    rootChain.either<NavigationChain<ViewConfig>, NavigationNode<out ViewConfig, ViewConfig>>().changesInSubtreeFlow().collect {
                        println(it)
                    }
                }
                InjectNavigationChain<ViewConfig> {
                    InjectNavigationNode(
                        PanelViewConfig()
                    )
                }
            }
        }
    }
}

package space.kscience.frameswork.features.ui.panel

import dev.inmo.micro_utils.koin.getAllDistinct
import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.core.NavigationNodeFactory
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module
import space.kscience.frameswork.features.common.web.models.ViewConfig
import space.kscience.frameswork.features.ui.panel.ui.PanelView
import space.kscience.frameswork.features.ui.panel.ui.PanelViewConfig
import space.kscience.frameswork.features.ui.panel.ui.PanelViewConfigProvider
import space.kscience.frameswork.features.ui.panel.ui.sample.SamplePanelViewConfigProvider

object JSPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(Plugin) { setupDI(config) }

        singleWithRandomQualifier<NavigationNodeFactory<ViewConfig>> {
            NavigationNodeFactory.Typed<PanelViewConfig, ViewConfig> { chain, config ->
                PanelView(chain, config)
            }
        }

        singleWithRandomQualifier<PanelViewConfigProvider> {
            SamplePanelViewConfigProvider
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
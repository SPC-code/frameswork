package space.kscience.frameswork.features.ui.panel.frames

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.core.NavigationNodeFactory
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module
import space.kscience.frameswork.features.common.web.models.ViewConfig
import space.kscience.frameswork.features.ui.panel.frames.ui.camera.CameraPanelViewConfigProvider
import space.kscience.frameswork.features.ui.panel.frames.ui.camera.CameraView
import space.kscience.frameswork.features.ui.panel.frames.ui.camera.CameraViewConfig
import space.kscience.frameswork.features.ui.panel.ui.PanelViewConfigProvider

object JSPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(Plugin) { setupDI(config) }

        singleWithRandomQualifier<NavigationNodeFactory<ViewConfig>> {
            NavigationNodeFactory.Typed<CameraViewConfig, ViewConfig> { chain, config ->
                CameraView(chain, config)
            }
        }

        singleWithRandomQualifier<PanelViewConfigProvider> { CameraPanelViewConfigProvider(get()) }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
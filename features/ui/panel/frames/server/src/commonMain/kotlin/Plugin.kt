package space.kscience.frameswork.features.ui.panel.frames.server

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module
import space.kscience.frameswork.features.frames.common.services.FramesSourcesCollector
import space.kscience.frameswork.features.ui.panel.frames.common.features.PanelCameraInfoFeature
import space.kscience.frameswork.features.ui.panel.frames.server.features.ServerPanelCameraInfoFeature

object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        single {
            ServerPanelCameraInfoFeature(
                processorsContainer = get(),
                framesSourcesCollector = get<FramesSourcesCollector>()
            )
        }
        single<PanelCameraInfoFeature> { get<ServerPanelCameraInfoFeature>() }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
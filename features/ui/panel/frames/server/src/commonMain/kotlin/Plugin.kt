package space.kscience.frameswork.features.ui.panel.frames.server

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module
import space.kscience.frameswork.features.frames.common.services.FramesSourcesCollector
import space.kscience.frameswork.features.ui.panel.frames.common.features.FramesDataInfoFeature
import space.kscience.frameswork.features.ui.panel.frames.server.features.ServerFramesDataInfoFeature

object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        single {
            ServerFramesDataInfoFeature(
                processorsContainer = get(),
                framesSourcesCollector = get<FramesSourcesCollector>()
            )
        }
        single<FramesDataInfoFeature> { get<ServerFramesDataInfoFeature>() }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
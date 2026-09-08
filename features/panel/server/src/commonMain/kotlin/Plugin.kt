package space.kscience.frameswork.features.panel.server

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module
import space.kscience.frameswork.features.panel.common.PanelFeature

object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        single { InMemoryPanelFeature() }
        single { GlobalKVBindedPanelFeature(globalKVRepo = get(), json = get()) }
        single<PanelFeature> { get<GlobalKVBindedPanelFeature>() }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
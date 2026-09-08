package space.kscience.frameswork.features.panel.web

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module
import space.kscience.frameswork.features.panel.common.PanelConstants
import space.kscience.frameswork.features.panel.common.PanelFeature

object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        single { KtorPanelFeature(get()) }
        single<PanelFeature> { get<KtorPanelFeature>() }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
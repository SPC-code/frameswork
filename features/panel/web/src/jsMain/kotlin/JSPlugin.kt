package space.kscience.frameswork.features.panel.web

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

object JSPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(space.kscience.frameswork.features.panel.common.JSPlugin) { setupDI(config) }
        with(Plugin) { setupDI(config) }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        space.kscience.frameswork.features.panel.common.JSPlugin.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
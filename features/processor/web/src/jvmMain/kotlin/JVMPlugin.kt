package space.kscience.frameswork.features.processor.web

import space.kscience.frameswork.features.processor.common.JVMPlugin.setupDI
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(space.kscience.frameswork.features.processor.common.JVMPlugin) { setupDI(config) }
        with(Plugin) { setupDI(config) }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        space.kscience.frameswork.features.processor.common.JVMPlugin.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
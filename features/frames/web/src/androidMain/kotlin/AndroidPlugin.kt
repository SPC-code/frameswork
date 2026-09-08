package space.kscience.frameswork.features.frames.web

import space.kscience.frameswork.features.frames.common.AndroidPlugin
import space.kscience.frameswork.features.frames.common.AndroidPlugin.setupDI
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

object AndroidPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(space.kscience.frameswork.features.frames.common.AndroidPlugin) { setupDI(config) }
        with(Plugin) { setupDI(config) }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        space.kscience.frameswork.features.frames.common.AndroidPlugin.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
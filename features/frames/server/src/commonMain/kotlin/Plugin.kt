package space.kscience.frameswork.features.frames.server

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
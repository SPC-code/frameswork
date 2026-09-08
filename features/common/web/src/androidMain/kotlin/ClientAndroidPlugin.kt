package space.kscience.frameswork.features.common.web

import space.kscience.frameswork.features.common.common.CommonAndroidPlugin
import space.kscience.frameswork.features.common.common.CommonAndroidPlugin.setupDI
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module

object ClientAndroidPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(CommonAndroidPlugin) { setupDI(config) }
        with(ClientPlugin) { setupDI(config) }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        CommonAndroidPlugin.startPlugin(koin)
        ClientPlugin.startPlugin(koin)
    }
}

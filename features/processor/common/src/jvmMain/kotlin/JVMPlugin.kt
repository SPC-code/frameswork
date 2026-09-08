package space.kscience.frameswork.features.processor.common

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module
import space.kscience.frameswork.features.processor.common.services.FramesProcessorMiddleware
import space.kscience.frameswork.features.processor.common.services.middlewares.BufferedImageSaverMiddleware

object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with (Plugin) { setupDI(config) }
        singleWithRandomQualifier<FramesProcessorMiddleware> { BufferedImageSaverMiddleware("./local/") }

        singleWithRandomQualifier<FramesProcessorMiddleware.Factory> { BufferedImageSaverMiddleware.Factory("./local/") }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
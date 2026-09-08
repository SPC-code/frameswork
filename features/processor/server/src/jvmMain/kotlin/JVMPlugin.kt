package space.kscience.frameswork.features.processor.server

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module
import space.kscience.frameswork.features.processor.common.services.FramesProcessorMiddleware
import space.kscience.frameswork.features.processor.server.processor_middleware.FrameCroppingMiddleware

object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(space.kscience.frameswork.features.processor.common.JVMPlugin) { setupDI(config) }
        with(Plugin) { setupDI(config) }

        (config["middlewares"] as? JsonObject) ?.let {
            (it["crops"] as? JsonObject) ?.let { it ->
                it.map {
                    singleWithRandomQualifier<FramesProcessorMiddleware.Factory> { _ ->
                        FrameCroppingMiddleware.Factory(
                            get<Json>().decodeFromJsonElement(FrameCroppingMiddleware.CropData.serializer(), it.value),
                            it.key
                        )
                    }
                }
            }
        }


    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        space.kscience.frameswork.features.processor.common.JVMPlugin.startPlugin(koin)
        Plugin.startPlugin(koin)

        // TEST PART

//        val scope = koin.get<CoroutineScope>()
//        val framesProcessor = koin.get<FramesProcessor>()
//        framesProcessor
//            .sourcesListUpdatesFlow
//            .map {
//                it
//                    .mapNotNull {
//                        framesProcessor.allocateFramesFlow(it)
//                    }
//            }
//            .subscribeLoggingDropExceptions(scope) {
//                it.forEach {
//                    it.subscribeLoggingDropExceptions(scope) {
//                        println(it.toString())
//                    }
//                }
//            }
    }
}
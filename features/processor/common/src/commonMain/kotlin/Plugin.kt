package space.kscience.frameswork.features.processor.common

import dev.inmo.micro_utils.koin.getAllDistinct
import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.koin.core.Koin
import org.koin.core.module.Module
import space.kscience.frameswork.features.processor.common.services.FramesProcessor
import space.kscience.frameswork.features.processor.common.services.FramesProcessorMiddleware
import space.kscience.frameswork.features.processor.common.services.FramesProcessorService

object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
//        single {
//            FramesProcessorService(
//                framesCollector = get(),
//                middlewares = getAllDistinct(),
//                scope = get()
//            )
//        }
//        single<FramesProcessor> {
//            get<FramesProcessorService>()
//        }

        singleWithRandomQualifier {
            SerializersModule {
                polymorphic(Any::class, FramesProcessorMiddleware.Factory.Simple::class, FramesProcessorMiddleware.Factory.Simple.serializer())
                polymorphic(FramesProcessorMiddleware.Factory::class, FramesProcessorMiddleware.Factory.Simple::class, FramesProcessorMiddleware.Factory.Simple.serializer())
            }
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}

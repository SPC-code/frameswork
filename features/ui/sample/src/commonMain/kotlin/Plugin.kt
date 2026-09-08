package space.kscience.frameswork.features.ui.sample

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.Koin
import org.koin.core.module.Module
import space.kscience.frameswork.features.common.web.models.ViewConfig
import space.kscience.frameswork.features.ui.sample.ui.SampleModel
import space.kscience.frameswork.features.ui.sample.ui.SampleViewConfig
import space.kscience.frameswork.features.ui.sample.ui.SampleViewModel

object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        singleWithRandomQualifier {
            SerializersModule {
                polymorphic(Any::class, SampleViewConfig::class, SampleViewConfig.serializer())
                polymorphic(ViewConfig::class, SampleViewConfig::class, SampleViewConfig.serializer())
            }
        }
        factory { SampleViewModel(it.get(), get()) }
        single<SampleModel> {
            object : SampleModel {

            }
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
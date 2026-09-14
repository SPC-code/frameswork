package space.kscience.frameswork.features.ui.panel.frames

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import korlibs.time.milliseconds
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.Koin
import org.koin.core.module.Module
import org.koin.core.qualifier.StringQualifier
import space.kscience.frameswork.features.frames.common.models.FramesSourceId
import space.kscience.frameswork.features.common.web.models.ViewConfig
import space.kscience.frameswork.features.frames.common.models.ByteArrayFrameData
import space.kscience.frameswork.features.ui.panel.frames.common.features.FramesDataInfoFeature
import space.kscience.frameswork.features.ui.panel.frames.features.CacheFramesDataInfoFeature
import space.kscience.frameswork.features.ui.panel.frames.features.GetFramesBackPressurePanelCameraInfoFeature
import space.kscience.frameswork.features.ui.panel.frames.ktor.KtorFramesDataInfoFeature
import space.kscience.frameswork.features.ui.panel.frames.ui.camera.CameraModel
import space.kscience.frameswork.features.ui.panel.frames.ui.camera.CameraViewConfig
import space.kscience.frameswork.features.ui.panel.frames.ui.camera.CameraViewModel

object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        singleWithRandomQualifier {
            SerializersModule {
                polymorphic(Any::class, CameraViewConfig::class, CameraViewConfig.serializer())
                polymorphic(ViewConfig::class, CameraViewConfig::class, CameraViewConfig.serializer())
            }
        }

        single { KtorFramesDataInfoFeature(client = get(), json = get(), scope = get()) }
        single<FramesDataInfoFeature>(StringQualifier("back_pressure")) { GetFramesBackPressurePanelCameraInfoFeature(original = get<KtorFramesDataInfoFeature>(), 30.milliseconds) }
        single { CacheFramesDataInfoFeature(fallback = get<FramesDataInfoFeature>(StringQualifier("back_pressure")), scope = get()) }
        single<FramesDataInfoFeature> { get<CacheFramesDataInfoFeature>() }

        factory { CameraViewModel(it.get(), get()) }
        single<CameraModel> {
            val feature = get<FramesDataInfoFeature>()
            object : CameraModel {
                override suspend fun getAvailableProcessors(): Set<String> = feature.getAvailableProcessors()
                override suspend fun getAvailableCameras(processorName: String): Set<FramesSourceId>? = feature.getAvailableFramesSources(processorName)

                override fun getCameraFrames(processorName: String, id: FramesSourceId): Flow<ByteArrayFrameData> {
                    return feature.getFramesFlow(processorName, id)
                }
            }
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
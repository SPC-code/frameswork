package space.kscience.frameswork.features.frames.common

import dev.inmo.micro_utils.common.DateTimeSerializer
import dev.inmo.micro_utils.koin.single
import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.micro_utils.repos.cache.full.direct.directlyFullyCached
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import korlibs.time.DateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.Koin
import org.koin.core.module.Module
import org.koin.core.qualifier.StringQualifier
import space.kscience.frameswork.features.frames.common.models.FrameSourceConnectorConfig
import space.kscience.frameswork.features.frames.common.models.FramesSourceId
import space.kscience.frameswork.features.frames.common.models.FFMPEGRTSPConfig
import space.kscience.frameswork.features.frames.common.services.FrameSourceConnector
import space.kscience.frameswork.features.frames.common.services.FramesCollector
import space.kscience.frameswork.features.frames.common.services.FramesSourcesCollector
import space.kscience.frameswork.features.frames.common.services.KVBasedFramesSourcesCollector
import space.kscience.frameswork.features.frames.common.services.MutableFramesSourcesCollector
import space.kscience.frameswork.features.frames.common.services.connectors.ExposedKVCamerasCollectorsRepo

object JVMPlugin : StartPlugin {
    @Serializable
    data class Config(
        val presets: List<FrameSourceConnectorConfig>
    )

    override fun Module.setupDI(config: JsonObject) {
        with (Plugin) { setupDI(config) }
        val subconfig = config["cameras"] ?: return

        single {
            SerializersModule {
                polymorphic(Any::class, FFMPEGRTSPConfig::class, FFMPEGRTSPConfig.serializer())
                polymorphic(FrameSourceConnectorConfig::class, FFMPEGRTSPConfig::class, FFMPEGRTSPConfig.serializer())
            }
        }

        single { get<Json>().decodeFromJsonElement(Config.serializer(), subconfig) }

        single<KeyValueRepo<FramesSourceId, FrameSourceConnector>>("exposed_cameras_collector") { ExposedKVCamerasCollectorsRepo(database = get(), json = get()) }
        single<KeyValueRepo<FramesSourceId, FrameSourceConnector>>("exposed_cached_cameras_collector") {
            get<KeyValueRepo<FramesSourceId, FrameSourceConnector>>(StringQualifier("exposed_cameras_collector"))
                .directlyFullyCached(
                    scope = get()
                )
        }
        single<KVBasedFramesSourcesCollector> {
            KVBasedFramesSourcesCollector(scope = get(), kvRepo = get(StringQualifier("exposed_cached_cameras_collector")))
        }
        single<FramesSourcesCollector> { get<KVBasedFramesSourcesCollector>() }
        single<MutableFramesSourcesCollector> { get<KVBasedFramesSourcesCollector>() }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        Plugin.startPlugin(koin)

        val config = koin.get<Config>()
        val collector = koin.get<MutableFramesSourcesCollector>()
        config.presets.forEach {
            val connector = it.createFramesSourceConnector()
            if (collector.getAvailableFramesSourcesIds().contains(connector.id)) return@forEach
            collector.addCamera(connector)
        }
    }
}

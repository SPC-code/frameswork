package space.kscience.frameswork.features.processor.server

import dev.inmo.micro_utils.koin.getAllDistinct
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module
import space.kscience.frameswork.features.processor.common.services.FramesProcessorMiddleware

object Plugin : StartPlugin {
    @Serializable
    data class Config(
        val processors: Map<String, ProcessorsContainer.ProcessorConfig>,
    )
    override fun Module.setupDI(config: JsonObject) {
        val configSection = config["processors"]
        if (configSection != null) {
            single<Config> {
                get<Json>().decodeFromJsonElement(Config.serializer(), config)
            }
        }
        single(createdAtStart = true) {
            val factories = getAllDistinct<FramesProcessorMiddleware.Factory>()
            ProcessorsContainer(
                framesCollector = get(),
                scope = get(),
                processorsConfigs = getOrNull<Config>() ?.processors ?: error(
                    "Unable to find `processors` section. Available factories: ${factories.map { it.id.string }.joinToString(", ")}"
                ),
                middlewaresFactories = factories
            )
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
package space.kscience.frameswork.features.panel.server

import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module
import space.kscience.frameswork.features.panel.common.PanelFeature
import space.kscience.frameswork.features.panel.common.models.PanelInfo

object Plugin : StartPlugin {
    @Serializable
    data class DefaultPanelInfoConfig(val default: PanelInfo)
    override fun Module.setupDI(config: JsonObject) {
        config["panel_info"] ?.let {
            single { _ -> get<Json>().decodeFromJsonElement(DefaultPanelInfoConfig.serializer(), it) }
        }
        single {
            val defaultPanel = getOrNull<DefaultPanelInfoConfig>() ?.default
            InMemoryPanelFeature(
                defaultPanel = defaultPanel,
                json = get()
            )
        }
        single {
            val defaultPanel = getOrNull<DefaultPanelInfoConfig>() ?.default
            GlobalKVBindedPanelFeature(globalKVRepo = get(), json = get(), defaultPanel = defaultPanel)
        }
        single<PanelFeature> { get<GlobalKVBindedPanelFeature>() }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
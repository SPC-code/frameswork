package space.kscience.frameswork.features.panel.server

import dev.inmo.micro_utils.repos.set
import kotlinx.serialization.json.Json
import space.kscience.frameswork.features.common.server.repos.GlobalKVRepo
import space.kscience.frameswork.features.panel.common.PanelFeature
import space.kscience.frameswork.features.panel.common.models.PanelInfo

class GlobalKVBindedPanelFeature(
    private val globalKVRepo: GlobalKVRepo,
    private val json: Json
) : PanelFeature {
    private val key = "panel_config"
    override suspend fun getPanelConfig(): PanelInfo? {
        return json.decodeFromString(
            PanelInfo.serializer(),
            globalKVRepo.get(key) ?: return null
        )
    }

    override suspend fun setPanelConfig(config: PanelInfo): Boolean {
        globalKVRepo.set(key, json.encodeToString(PanelInfo.serializer(), config))
        return getPanelConfig() == config
    }
}
package space.kscience.frameswork.features.panel.common

import space.kscience.frameswork.features.panel.common.models.PanelInfo

interface PanelFeature {
    suspend fun getPanelConfig(): PanelInfo?
    suspend fun getDefaultConfig(): PanelInfo?
    suspend fun setPanelConfig(config: PanelInfo): Boolean
}

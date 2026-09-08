package space.kscience.frameswork.features.ui.panel.ui

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import space.kscience.frameswork.features.panel.common.models.PanelInfo

interface PanelModel {
    val decodingJson: Json
    fun getPanelInfoFlow(): Flow<PanelInfo>
    suspend fun updatePanelInfo(info: PanelInfo)
}
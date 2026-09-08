package space.kscience.frameswork.features.panel.common.models

import kotlinx.serialization.Serializable

@Serializable
data class PanelInfo(
    val horizontalSlots: Int,
    val verticalSlots: Int,
    val items: List<PanelItemInfo>
)

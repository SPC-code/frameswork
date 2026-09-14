package space.kscience.frameswork.features.panel.common.models

import kotlinx.serialization.Serializable

/**
 * A serializable description of a panel grid and the items placed on it.
 *
 * This value does not validate that the dimensions are positive or that [items] fit inside the
 * declared grid.
 *
 * @property horizontalSlots number of slots along the horizontal axis.
 * @property verticalSlots number of slots along the vertical axis.
 * @property items items placed in the panel.
 */
@Serializable
data class PanelInfo(
    val horizontalSlots: Int,
    val verticalSlots: Int,
    val items: List<PanelItemInfo>
)

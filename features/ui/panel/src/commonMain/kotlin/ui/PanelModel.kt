package space.kscience.frameswork.features.ui.panel.ui

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import space.kscience.frameswork.features.panel.common.models.PanelInfo

/**
 * Supplies panel layouts to the UI and accepts complete layout replacements.
 *
 * Implementations control where layouts are stored. The [decodingJson] instance must be able to
 * decode every polymorphic item configuration emitted by [getPanelInfoFlow].
 */
interface PanelModel {
    /** JSON instance used by the panel view to decode item-specific view configurations. */
    val decodingJson: Json

    /** Returns the stream of panel layouts that should be displayed. */
    fun getPanelInfoFlow(): Flow<PanelInfo>

    /** Requests that the current panel layout be replaced with [info]. */
    suspend fun updatePanelInfo(info: PanelInfo)
}

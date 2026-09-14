package space.kscience.frameswork.features.ui.panel.ui

import kotlinx.serialization.Serializable
import space.kscience.frameswork.features.common.web.models.ViewConfig

/**
 * Serializable navigation configuration that opens the editable panel view.
 *
 * The configuration carries no per-view fields; the panel layout is obtained through
 * [PanelModel] after the navigation node has been created.
 */
@Serializable
class PanelViewConfig : ViewConfig {

}

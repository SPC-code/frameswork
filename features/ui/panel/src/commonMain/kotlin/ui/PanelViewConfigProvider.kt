package space.kscience.frameswork.features.ui.panel.ui

import androidx.compose.runtime.Composable
import space.kscience.frameswork.features.common.web.models.ViewConfig

/**
 * Renders an editor that creates one kind of panel-item [ViewConfig].
 *
 * Multiple implementations may be contributed to dependency injection. The panel lets the user
 * select one provider before placing a new item in the grid.
 */
interface PanelViewConfigProvider {
    /** Localized title shown for this provider in the configuration-provider selector. */
    val title: String
        @Composable get

    /**
     * Draws the configuration editor and reports its current result through [reportState].
     *
     * The editor should report a concrete [ViewConfig] whenever its inputs form a valid
     * configuration and `null` while the form is incomplete or invalid.
     *
     * @param reportState callback to invoke whenever the prospective configuration changes.
     */
    @Composable
    fun Draw(reportState: (ViewConfig?) -> Unit)
}

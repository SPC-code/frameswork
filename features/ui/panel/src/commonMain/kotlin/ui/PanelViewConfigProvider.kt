package space.kscience.frameswork.features.ui.panel.ui

import androidx.compose.runtime.Composable
import space.kscience.frameswork.features.common.web.models.ViewConfig

interface PanelViewConfigProvider {
    val title: String
        @Composable get
    /**
     * Drawing configuration of the [ViewConfig] it may create. Must call [reportState] when state of config is changed.
     * In case when config is correctly fulfilled, it will pass a valid [ViewConfig] instance. In case when form
     * is incomplete - will pass null.
     */
    @Composable
    fun Draw(reportState: (ViewConfig?) -> Unit)
}

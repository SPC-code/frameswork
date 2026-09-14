package space.kscience.frameswork.features.ui.panel.ui

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.launchLoggingDropExceptions
import dev.inmo.micro_utils.coroutines.runCatchingLogging
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.mvvm.ViewModel
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import space.kscience.frameswork.features.common.web.models.ViewConfig
import space.kscience.frameswork.features.panel.common.models.PanelInfo
import space.kscience.frameswork.features.panel.common.models.PanelItemInfo

/**
 * Coordinates panel loading, editing, item creation, import, and export for a navigation node.
 *
 * @property node navigation node that owns this view model.
 * @property model source and persistence boundary for panel layouts.
 * @property panelViewConfigProviders editors available when a user adds a panel item.
 * @property json JSON instance used to encode and decode complete panel configurations and newly
 * created item configurations.
 */
class PanelViewModel(
    private val node: NavigationNode<PanelViewConfig, ViewConfig>,
    private val model: PanelModel,
    private val panelViewConfigProviders: List<PanelViewConfigProvider>,
    private val json: Json
) : ViewModel<ViewConfig>(node) {
    /** Mutable backing state for the last layout emitted by [model]. */
    private val _panelState = MutableRedeliverStateFlow<PanelInfo?>(null)

    /** Latest persisted layout, or `null` until [model] emits its first value. */
    val panelState = _panelState.asStateFlow()

    /** JSON instance supplied by [model] for decoding panel-item navigation configurations. */
    val decodingJson = model.decodingJson

    /** Mutable backing state for the available item-configuration providers. */
    private val _availablePanelViewConfigProviders = MutableRedeliverStateFlow<List<PanelViewConfigProvider>>(panelViewConfigProviders)

    /** Item-configuration providers offered by the add-item UI. */
    val availablePanelViewConfigProviders = _availablePanelViewConfigProviders.asStateFlow()

    /** Mutable backing state for the coordinates targeted by the add-item workflow. */
    private val _newPaneViewConfigXY = MutableRedeliverStateFlow<Pair<Int, Int>?>(null)

    /** Target `(x, y)` coordinates for a new item, or `null` when no item is being added. */
    val newPaneViewConfigXY = _newPaneViewConfigXY.asStateFlow()

    /** Mutable backing state for the provider selected by the add-item workflow. */
    private val _selectedPanelViewConfigProvider = MutableRedeliverStateFlow<PanelViewConfigProvider?>(null)

    /** Provider currently selected to configure a new panel item. */
    val selectedPanelViewConfigProvider = _selectedPanelViewConfigProvider.asStateFlow()

    /** Mutable backing state for the new item configuration reported by the selected provider. */
    private val _potentialNewConfig = MutableRedeliverStateFlow<ViewConfig?>(null)

    /** Valid prospective item configuration, or `null` while the provider form is incomplete. */
    val potentialNewConfig = _potentialNewConfig.asStateFlow()

    /** Mutable backing state for the editable layout copy. */
    private val _currentlyEditedPanelState = MutableRedeliverStateFlow<PanelInfo?>(null)

    /** Layout being edited, or `null` when the panel is in view mode. */
    val currentlyEditedPanelState = _currentlyEditedPanelState.asStateFlow()

    init {
        model.getPanelInfoFlow().subscribeLoggingDropExceptions(scope) {
            _panelState.value = it
        }
    }

    /**
     * Persists the edited layout asynchronously and exits edit mode.
     *
     * The call has no effect when edit mode is inactive.
     */
    fun savePanel() {
        val newPanel = _currentlyEditedPanelState.value ?: return
        scope.launchLoggingDropExceptions {
            model.updatePanelInfo(newPanel)
        }
        _currentlyEditedPanelState.value = null
    }

    /** Discards the editable layout and exits edit mode. */
    fun onCancelEditing() {
        _currentlyEditedPanelState.value = null
    }

    /** Starts the add-item workflow for grid cell [x], [y]. */
    fun startAddConfigProcedure(x: Int, y: Int) {
        _newPaneViewConfigXY.value = x to y
    }

    /** Records the latest [config] reported by the selected item-configuration provider. */
    fun onNewConfigReported(config: ViewConfig?) {
        _potentialNewConfig.value = config
    }

    /** Clears all transient state associated with adding a panel item. */
    fun stopAddConfigProcedure() {
        _newPaneViewConfigXY.value = null
        _selectedPanelViewConfigProvider.value = null
        _potentialNewConfig.value = null
    }

    /**
     * Appends the prospective configuration as a one-by-one item at the selected coordinates.
     *
     * The call has no effect unless editing is active, coordinates have been selected, and the
     * provider has reported a valid configuration. The add-item workflow is closed after a
     * successful append.
     */
    fun saveNewConfig() {
        val config = _potentialNewConfig.value ?: return
        _currentlyEditedPanelState.value = _currentlyEditedPanelState.value ?.let {
            it.copy(
                items = it.items + PanelItemInfo(
                    x = _newPaneViewConfigXY.value ?.first ?: return,
                    y = _newPaneViewConfigXY.value ?.second ?: return,
                    width = 1,
                    height = 1,
                    config = config,
                    json = json
                )
            )
        }
        stopAddConfigProcedure()
    }

    /** Copies the latest persisted layout into editable state, entering edit mode when available. */
    fun onEnterEditing() {
        _currentlyEditedPanelState.value = _panelState.value
    }

    /** Replaces the in-progress editable layout with [newPanelState]. */
    fun updateEditingPanel(
        newPanelState: PanelInfo
    ) {
        _currentlyEditedPanelState.value = newPanelState
    }

    /** Selects [provider] as the editor used to configure the prospective panel item. */
    fun onNewConfigProviderSelected(provider: PanelViewConfigProvider) {
        _selectedPanelViewConfigProvider.value = provider
    }

    /**
     * Encodes the editable layout as JSON.
     *
     * @return the serialized layout, or `null` when edit mode is inactive.
     */
    fun exportCurrentConfig(): String? {
        return json.encodeToString(PanelInfo.serializer(), _currentlyEditedPanelState.value ?: return null)
    }

    /**
     * Attempts to decode [config] as a panel layout and replace the editable layout with it.
     *
     * Decoding failures are logged and leave the existing editable state unchanged.
     */
    fun importConfig(config: String) {
        runCatchingLogging {
            _currentlyEditedPanelState.value = json.decodeFromString(PanelInfo.serializer(), config)
        }
    }
}

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

class PanelViewModel(
    private val node: NavigationNode<PanelViewConfig, ViewConfig>,
    private val model: PanelModel,
    private val panelViewConfigProviders: List<PanelViewConfigProvider>,
    private val json: Json
) : ViewModel<ViewConfig>(node) {
    private val _panelState = MutableRedeliverStateFlow<PanelInfo?>(null)
    val panelState = _panelState.asStateFlow()
    val decodingJson = model.decodingJson

    private val _availablePanelViewConfigProviders = MutableRedeliverStateFlow<List<PanelViewConfigProvider>>(panelViewConfigProviders)
    val availablePanelViewConfigProviders = _availablePanelViewConfigProviders.asStateFlow()

    private val _newPaneViewConfigXY = MutableRedeliverStateFlow<Pair<Int, Int>?>(null)
    val newPaneViewConfigXY = _newPaneViewConfigXY.asStateFlow()
    private val _selectedPanelViewConfigProvider = MutableRedeliverStateFlow<PanelViewConfigProvider?>(null)
    val selectedPanelViewConfigProvider = _selectedPanelViewConfigProvider.asStateFlow()

    private val _potentialNewConfig = MutableRedeliverStateFlow<ViewConfig?>(null)
    val potentialNewConfig = _potentialNewConfig.asStateFlow()

    private val _currentlyEditedPanelState = MutableRedeliverStateFlow<PanelInfo?>(null)
    val currentlyEditedPanelState = _currentlyEditedPanelState.asStateFlow()

    init {
        model.getPanelInfoFlow().subscribeLoggingDropExceptions(scope) {
            _panelState.value = it
        }
    }

    fun savePanel() {
        val newPanel = _currentlyEditedPanelState.value ?: return
        scope.launchLoggingDropExceptions {
            model.updatePanelInfo(newPanel)
        }
        _currentlyEditedPanelState.value = null
    }
    fun onCancelEditing() {
        _currentlyEditedPanelState.value = null
    }

    fun startAddConfigProcedure(x: Int, y: Int) {
        _newPaneViewConfigXY.value = x to y
    }
    fun onNewConfigReported(config: ViewConfig?) {
        _potentialNewConfig.value = config
    }

    fun stopAddConfigProcedure() {
        _newPaneViewConfigXY.value = null
        _selectedPanelViewConfigProvider.value = null
        _potentialNewConfig.value = null
    }

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

    fun onEnterEditing() {
        _currentlyEditedPanelState.value = _panelState.value
    }
    fun updateEditingPanel(
        newPanelState: PanelInfo
    ) {
        _currentlyEditedPanelState.value = newPanelState
    }

    fun onNewConfigProviderSelected(provider: PanelViewConfigProvider) {
        _selectedPanelViewConfigProvider.value = provider
    }

    fun exportCurrentConfig(): String? {
        return json.encodeToString(PanelInfo.serializer(), _currentlyEditedPanelState.value ?: return null)
    }

    fun importConfig(config: String) {
        runCatchingLogging {
            _currentlyEditedPanelState.value = json.decodeFromString(PanelInfo.serializer(), config)
        }
    }
}
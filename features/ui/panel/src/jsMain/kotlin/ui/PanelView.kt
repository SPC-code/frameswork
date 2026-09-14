package space.kscience.frameswork.features.ui.panel.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.benasher44.uuid.uuid4
import dev.inmo.kslog.common.e
import dev.inmo.micro_utils.colors.black
import dev.inmo.micro_utils.colors.common.HEXAColor
import dev.inmo.micro_utils.colors.dimgray
import dev.inmo.micro_utils.common.compose.plus
import dev.inmo.micro_utils.common.compose.tagClasses
import dev.inmo.micro_utils.common.readBytes
import dev.inmo.micro_utils.common.readBytesPromise
import dev.inmo.micro_utils.common.selectFile
import dev.inmo.micro_utils.common.triggerDownloadFile
import dev.inmo.micro_utils.coroutines.compose.StyleSheetsAggregator
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.compose.InjectNavigationChain
import dev.inmo.navigation.compose.InjectNavigationNode
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import kotlinx.browser.window
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.Draggable
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.CSSColorValue
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.LineStyle
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.StyleSheet
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.border
import org.jetbrains.compose.web.css.bottom
import org.jetbrains.compose.web.css.color
import org.jetbrains.compose.web.css.cursor
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.gap
import org.jetbrains.compose.web.css.gridColumn
import org.jetbrains.compose.web.css.gridRow
import org.jetbrains.compose.web.css.gridTemplateColumns
import org.jetbrains.compose.web.css.gridTemplateRows
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.left
import org.jetbrains.compose.web.css.maxHeight
import org.jetbrains.compose.web.css.maxWidth
import org.jetbrains.compose.web.css.overflow
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.right
import org.jetbrains.compose.web.css.style
import org.jetbrains.compose.web.css.textAlign
import org.jetbrains.compose.web.css.top
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Li
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Ul
import org.koin.core.parameter.parametersOf
import space.kscience.frameswork.features.common.web.models.ViewConfig
import org.koin.core.component.inject
import org.w3c.dom.events.Event
import org.w3c.dom.url.URL
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import space.kscience.frameswork.features.common.web.utils.cssRGBA
import space.kscience.frameswork.features.ui.panel.PanelStrings

/**
 * Compose Web navigation view that displays and edits a grid of child navigation views.
 *
 * Each panel item is decoded with [PanelViewModel.decodingJson] and rendered as a child node in the
 * supplied navigation chain. Edit mode supports adding, moving, resizing, deleting, importing,
 * and exporting items.
 *
 * @param chain navigation chain in which child item configurations are rendered.
 * @param config navigation configuration for this panel node.
 */
class PanelView(
    chain: NavigationChain<ViewConfig>,
    config: PanelViewConfig,
) : ComposeView<PanelViewConfig, ViewConfig, PanelViewModel>(config, chain) {
    /** View model resolved lazily with this view as the navigation-node factory parameter. */
    override val viewModel: PanelViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) { parametersOf(this@PanelView) }

    /** CSS rules used by the panel grid and its editing overlays. */
    private object PanelViewStylesheet : StyleSheet() {
        /** Absolutely positioned container that fills the available panel area. */
        val panelViewContainer by style {
            position(Position.Absolute)
            top(0.px)
            bottom(0.px)
            right(0.px)
            left(0.px)
        }

        /** Container that anchors panel-management controls to the top-right corner. */
        val managementButtonsContainer by style {
            position(Position.Absolute)
            top(0.px)
            right(0.px)
        }

        /** Grid that lays out panel items and edit controls. */
        val panelViewGrid by style {
            display(DisplayStyle.Grid)
            gap(8.px)
            position(Position.Absolute)
            top(0.px)
            bottom(0.px)
            right(0.px)
            left(0.px)
        }

        /** Dashed grid decoration shown while panel editing is active. */
        val panelViewGridDragging by style {
            border {
                style(LineStyle.Dashed)
                width(1.px)
                color(HEXAColor.black.cssRGBA)
            }
        }

        /** Translucent per-cell drop target displayed during dragging or resizing. */
        val panelViewGridDraggingOverlap by style {
            width(100.percent)
            height(100.percent)
            backgroundColor(HEXAColor.dimgray.copy(aOfOne = 0.5f).cssRGBA)
            border {
                style(LineStyle.Dashed)
                width(1.px)
            }
        }

        /** Overlay container that exposes resize handles without intercepting other pointer input. */
        val panelViewGridEditResizeCellContainer by style {
            width(100.percent)
            maxWidth(100.percent)
            height(100.percent)
            maxHeight(100.percent)
            position(Position.Relative)
            property("pointer-events", "none")
            overflow("hidden")
        }

        /** Clipped container for a rendered panel-item view. */
        val panelViewGridResizeCellContainer by style {
            width(100.percent)
            maxWidth(100.percent)
            height(100.percent)
            maxHeight(100.percent)
            position(Position.Relative)
            overflow("hidden")
        }

        /** Absolutely positioned resize handle along one side of an item. */
        val panelViewGridEditResizeCellSide by style {
            position(Position.Absolute)
            property("pointer-events", "auto")
        }

        /** Drop target used to remove a dragged panel item. */
        val panelViewGridTrashArea by style {
            backgroundColor(HEXAColor.dimgray.copy(aOfOne = 0.5f).cssRGBA)
            position(Position.Relative)
            display(DisplayStyle.Flex)
            justifyContent(JustifyContent.Center)
            alignItems(AlignItems.Center)
            textAlign("center")
            border {
                style(LineStyle.Dashed)
                width(3.px)
                color("var(--bs-danger)".unsafeCast<CSSColorValue>())
            }
        }

        init {
            StyleSheetsAggregator.addStyleSheet(this)
        }
    }

    /** Draws the provider selector used by the add-item dialog. */
    @Composable
    fun ConfigEditingConfigProvidersDropdown() {
        Div(attrs = { classes("dropdown") }) {
            val selectedProvider = viewModel.selectedPanelViewConfigProvider.collectAsState()
            val availableProviders = viewModel.availablePanelViewConfigProviders.collectAsState()
            if (availableProviders.value.isEmpty()) return@Div
            if (selectedProvider.value == null) {
                viewModel.onNewConfigProviderSelected(availableProviders.value.first())
            }
            Button(attrs = {
                classes("btn", "btn-secondary", "dropdown-toggle")
                type(ButtonType.Button)
                attr("data-bs-toggle", "dropdown")
                attr("aria-expanded", "false")
            }) {
                Text(selectedProvider.value ?.title ?: PanelStrings.loading.translation())
            }
            Ul(attrs = { classes("dropdown-menu") }) {
                availableProviders.value.forEach { configProvider ->
                    Li {
                        A(
                            href = "#",
                            attrs = {
                                classes("dropdown-item")
                                onClick { _ ->
                                    viewModel.onNewConfigProviderSelected(configProvider)
                                }
                            },
                        ) {
                            Text(configProvider.title)
                        }
                    }
                }
            }
        }
    }

    /** Draws and manages the modal editor for a prospective panel-item configuration. */
    @Composable
    fun ConfigEditingModal() {
        val id = remember { uuid4().toString().replace("-", "") }
        Div(attrs = {
            classes("modal", "fade", "show")
            id("staticBackdrop")
            attr("data-bs-backdrop", "static")
            attr("data-bs-keyboard", "false")
            tabIndex(-1)
            attr("aria-labelledby", "staticBackdropLabel")
            attr("aria-hidden", "true")
            id(id)
            ref {
                val modal = js("bootstrap").Modal.getOrCreateInstance(it)
                modal.show()
                onDispose { modal.hide() }
            }
            addEventListener("hide.bs.modal", { viewModel.stopAddConfigProcedure() })
        }) {
            Div(attrs = { classes("modal-dialog") }) {
                Div(attrs = { classes("modal-content") }) {
                    // Header
                    Div(attrs = { classes("modal-header") }) {
                        H1(attrs = {
                            classes("modal-title", "fs-5")
                            id("staticBackdropLabel")
                        }) {
                            Text(PanelStrings.addConfigTitle.translation())
                        }
//                        Button(attrs = {
//                            type(ButtonType.Button)
//                            classes("btn-close")
//                            attr("data-bs-dismiss", "modal")
//                            attr("aria-label", "Close")
//                        })
                    }
                    // Body
                    Div(attrs = { classes("modal-body") }) {
                        ConfigEditingConfigProvidersDropdown()
                        viewModel.selectedPanelViewConfigProvider.collectAsState().value ?.Draw {
                            viewModel.onNewConfigReported(it)
                        }
                    }
                    // Footer
                    Div(attrs = { classes("modal-footer") }) {
                        Button(attrs = {
                            type(ButtonType.Button)
                            classes("btn", "btn-secondary")
                            attr("data-bs-dismiss", "modal")
                        }) {
                            Text(PanelStrings.cancelConfigAdding.translation())
                        }
                        Button(attrs = {
                            type(ButtonType.Button)
                            classes("btn", "btn-primary")
                            onClick {
                                viewModel.saveNewConfig()
                            }
                        }) {
                            Text(PanelStrings.completeConfigAdding.translation())
                        }
                    }
                }
            }
        }
    }

    /** Draws the current panel and, when enabled, all grid-editing controls. */
    @Composable
    override fun onDraw() {
        super.onDraw()
        Div(tagClasses(PanelViewStylesheet.panelViewContainer)) {
            val panelStateValue = viewModel.panelState.collectAsState().value
            if (panelStateValue == null) {
                Div(tagClasses("grid", "text-center")) {
                    Div(
                        tagClasses("g-col-1", "g-start-6", "spinner-border", "text-info", "text-center") + {
                            attr("role", "status")
                        }
                    ) {
                        Span(
                            tagClasses("visually-hidden")
                        ) {
                            Text("Loading...")
                        }
                    }
                }
            } else {
                val currentlyEditedPanelState = viewModel.currentlyEditedPanelState.collectAsState()
                val panelCurrentState = currentlyEditedPanelState.value ?: panelStateValue
                val editMode = key(currentlyEditedPanelState.value) { currentlyEditedPanelState.value != null }
                Div(
                    key(
                        panelCurrentState.verticalSlots,
                        panelCurrentState.horizontalSlots
                    ) {
                        {
                            classes(PanelViewStylesheet.panelViewGrid)
                            if (editMode) {
                                classes(PanelViewStylesheet.panelViewGridDragging)
                            }
                            style {
                                val additionalRow = if (editMode) "38px 38px" else ""
                                val additionalColumn = if (editMode) "38px 38px" else ""
                                gridTemplateColumns("repeat(${panelCurrentState.verticalSlots}, minmax(0, 1fr)) $additionalColumn")
                                gridTemplateRows("repeat(${panelCurrentState.horizontalSlots}, minmax(0, 1fr)) $additionalRow")
                            }
                        }
                    }
                ) {
                    val draggingIndex = remember(panelStateValue, editMode) { mutableStateOf<Int?>(null) }
                    val resizingParameters = remember(panelStateValue, editMode) { mutableStateOf<Pair<Int, Int>?>(null) }
                    panelCurrentState.items.forEachIndexed { i, it ->
                        key(i, it) {
                            Div({
                                classes(PanelViewStylesheet.panelViewGridResizeCellContainer)
                                style {
                                    gridColumn("${1 + it.x} / ${1 + it.x + it.width}")
                                    gridRow("${1 + it.y} / ${1 + it.y + it.height}")
                                }

                                if (editMode) {
                                    onDragStart {
                                        val iData = i.toString()
                                        it.dataTransfer?.setData("text", iData)
                                        draggingIndex.value = i
                                    }

                                    draggable(Draggable.True)
                                }
                            }) {
                                InjectNavigationChain<ViewConfig> {
                                    InjectNavigationNode(it.decodeConfig(viewModel.decodingJson))
                                }
                            }
                            if (editMode) {
                                Div({
                                    classes(PanelViewStylesheet.panelViewGridEditResizeCellContainer)
                                    style {
                                        gridColumn("${1 + it.x} / ${1 + it.x + it.width}")
                                        gridRow("${1 + it.y} / ${1 + it.y + it.height}")
                                    }
                                }) {
                                    key(panelCurrentState.horizontalSlots, panelCurrentState.verticalSlots) {
                                        if (it.x > 0 || it.width > 1) {
                                            Div({
                                                classes(PanelViewStylesheet.panelViewGridEditResizeCellSide)
                                                style {
                                                    left(0.px)
                                                    top(0.px)
                                                    bottom(0.px)
                                                    width(10.px)
                                                    cursor("col-resize")
                                                }
                                                draggable(Draggable.True)
                                                onDragStart {
                                                    val iData = i.toString()
                                                    it.dataTransfer?.setData("text", iData)
                                                    draggingIndex.value = i
                                                    resizingParameters.value = Pair(-1, 0)
                                                }
                                            })
                                        }
                                        if (it.y > 0 || it.height > 1) {
                                            Div({
                                                classes(PanelViewStylesheet.panelViewGridEditResizeCellSide)
                                                style {
                                                    top(0.px)
                                                    left(0.px)
                                                    right(0.px)
                                                    height(10.px)
                                                    cursor("row-resize")
                                                }
                                                draggable(Draggable.True)
                                                onDragStart {
                                                    val iData = i.toString()
                                                    it.dataTransfer?.setData("text", iData)
                                                    draggingIndex.value = i
                                                    resizingParameters.value = Pair(0, -1)
                                                }
                                            })
                                        }
                                        if (it.x < panelCurrentState.verticalSlots - 1 || it.width > 1) {
                                            Div({
                                                classes(PanelViewStylesheet.panelViewGridEditResizeCellSide)
                                                style {
                                                    right(0.px)
                                                    top(0.px)
                                                    bottom(0.px)
                                                    width(10.px)
                                                    cursor("col-resize")
                                                }
                                                draggable(Draggable.True)
                                                onDragStart {
                                                    val iData = i.toString()
                                                    it.dataTransfer?.setData("text", iData)
                                                    draggingIndex.value = i
                                                    resizingParameters.value = Pair(1, 0)
                                                }
                                            })
                                        }
                                        if (it.y < panelCurrentState.horizontalSlots - 1 || it.height > 1) {
                                            Div({
                                                classes(PanelViewStylesheet.panelViewGridEditResizeCellSide)
                                                style {
                                                    bottom(0.px)
                                                    left(0.px)
                                                    right(0.px)
                                                    height(10.px)
                                                    cursor("row-resize")
                                                }
                                                draggable(Draggable.True)
                                                onDragStart {
                                                    val iData = i.toString()
                                                    it.dataTransfer?.setData("text", iData)
                                                    draggingIndex.value = i
                                                    resizingParameters.value = Pair(0, 1)
                                                }
                                            })
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val draggingIndexValue = draggingIndex.value
                    val resizingParametersValue = resizingParameters.value
                    if (editMode) {
                        DisposableEffect(editMode) {
                            val callback = { _: Event ->
                                draggingIndex.value = null
                                resizingParameters.value = null
                            }
                            window.addEventListener("mouseup", callback)
                            onDispose {
                                window.removeEventListener("mouseup", callback)
                            }
                        }
                    }
                    if (draggingIndexValue != null && editMode) {
                        for (x in 0 until panelCurrentState.verticalSlots) {
                            val x = x
                            for (y in 0 until panelCurrentState.horizontalSlots) {
                                val y = y
                                Div({
                                    classes(PanelViewStylesheet.panelViewGridDraggingOverlap)
                                    style {
                                        gridColumn("${x + 1} / ${x + 2}")
                                        gridRow("${y + 1} / ${y + 2}")
                                        position(Position.Relative)
                                    }
                                    onDragOver {
                                        it.preventDefault()
                                        when {
                                            resizingParametersValue == null -> {
                                                val item = panelCurrentState.items.getOrNull(draggingIndexValue) ?: return@onDragOver
                                                if (item.x == x && item.y == y) {
                                                    return@onDragOver
                                                } else {
                                                    viewModel.updateEditingPanel(
                                                        panelCurrentState.copy(
                                                            items = panelCurrentState.items.toMutableList().apply {
                                                                val item = this[draggingIndexValue]
                                                                this[draggingIndexValue] = item.copy(x = x, y = y)
                                                            }
                                                        )
                                                    )
                                                }
                                            }
                                            else -> {
                                                val item = panelCurrentState.items.getOrNull(draggingIndexValue) ?: return@onDragOver

                                                var newItem = item
                                                when {
                                                    resizingParametersValue.first == 0 -> {} // do nothing
                                                    else -> {
                                                        if (resizingParametersValue.first > 0) {
                                                            newItem = newItem
                                                                .copy(
                                                                    width = x - item.x + 1
                                                                )
                                                                .takeIf {
                                                                    it.width > 0
                                                                } ?: newItem
                                                        } else {
                                                            newItem = newItem
                                                                .copy(
                                                                    x = x,
                                                                    width = item.width + (item.x - x),
                                                                )
                                                                .takeIf {
                                                                    it.x >= 0 && it.width > 0
                                                                } ?: newItem
                                                        }
                                                    }
                                                }
                                                when {
                                                    resizingParametersValue.second == 0 -> {} // do nothing
                                                    else -> {
                                                        if (resizingParametersValue.second > 0) {
                                                            newItem = newItem
                                                                .copy(
                                                                    height = y - item.y + 1
                                                                )
                                                                .takeIf {
                                                                    it.height > 0
                                                                } ?: newItem
                                                        } else {
                                                            newItem = newItem
                                                                .copy(
                                                                    y = y,
                                                                    height = item.height + (item.y - y)
                                                                )
                                                                .takeIf {
                                                                    it.y >= 0 && it.height > 0
                                                                } ?: newItem
                                                        }
                                                    }
                                                }
                                                this@PanelView.log.e { newItem.toString() }
                                                if (item != newItem) {
                                                    viewModel.updateEditingPanel(
                                                        panelCurrentState.copy(
                                                            items = panelCurrentState
                                                                .items
                                                                .toMutableList()
                                                                .apply {
                                                                    this[draggingIndexValue] = newItem
                                                                }
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    onDrop {
                                        it.preventDefault()
                                        draggingIndex.value = null
                                        resizingParameters.value = null
                                    }
                                })
                            }
                        }
                    }
                    if (editMode && draggingIndex.value == null) {
                        for (x in 0 until panelCurrentState.verticalSlots) {
                            val x = x
                            for (y in 0 until panelCurrentState.horizontalSlots) {
                                val y = y
                                if (panelCurrentState.items.any { it.contains(x, y) }) continue

                                Button({
                                    style {
                                        gridColumn("${x + 1} / ${x + 2}")
                                        gridRow("${y + 1} / ${y + 2}")
                                    }
                                    classes("btn", "btn-outline-primary")
                                    onClick {
                                        viewModel.startAddConfigProcedure(x, y)
                                    }
                                }) {
                                    Text("+")
                                }
                            }
                        }
                    }
                    if (editMode) {
                        key(
                            panelCurrentState.verticalSlots,
                            panelCurrentState.horizontalSlots
                        ) {
                            for (x in 0 until panelCurrentState.verticalSlots) {
                                val x = x
                                Button({
                                    style {
                                        gridColumn("${x + 1} / ${x + 2}")
                                        gridRow("${panelCurrentState.horizontalSlots + 2} / ${panelCurrentState.horizontalSlots + 2}")
                                    }
                                    classes("btn", "btn-danger")
                                    onClick {
                                        val newPanelState = panelCurrentState.copy(
                                            verticalSlots = panelCurrentState.verticalSlots - 1,
                                            items = panelCurrentState.items.mapNotNull {
                                                when {
                                                    it.containsX(x) == false -> {
                                                        if (it.x > x) {
                                                            it.copy(x = it.x - 1)
                                                        } else {
                                                            it
                                                        }
                                                    }
                                                    it.width == 1 -> null
                                                    else -> it.copy(width = it.width - 1)
                                                }
                                            }
                                        )
                                        viewModel.updateEditingPanel(newPanelState)
                                    }
                                }) {
                                    Text("-")
                                }
                            }
                            for (y in 0 until panelCurrentState.horizontalSlots) {
                                val y = y
                                Button({
                                    style {
                                        gridColumn("${panelCurrentState.verticalSlots + 2} / ${panelCurrentState.verticalSlots + 2}")
                                        gridRow("${y + 1} / ${y + 2}")
                                    }
                                    classes("btn", "btn-danger")
                                    onClick {
                                        val newPanelState = panelCurrentState.copy(
                                            horizontalSlots = panelCurrentState.horizontalSlots - 1,
                                            items = panelCurrentState.items.mapNotNull {
                                                when {
                                                    it.containsY(y) == false -> {
                                                        if (it.y > y) {
                                                            it.copy(y = it.y - 1)
                                                        } else {
                                                            it
                                                        }
                                                    }
                                                    it.height == 1 -> null
                                                    else -> it.copy(height = it.height - 1)
                                                }
                                            }
                                        )
                                        viewModel.updateEditingPanel(newPanelState)
                                    }
                                }) {
                                    Text("-")
                                }
                            }
                            Button({
                                style {
                                    gridColumn("${1} / ${panelCurrentState.verticalSlots + 1}")
                                    gridRow("${panelCurrentState.horizontalSlots + 1} / ${panelCurrentState.horizontalSlots + 1}")
                                }
                                classes("btn", "btn-primary")
                                onClick {
                                    val newPanelState = panelCurrentState.copy(
                                        horizontalSlots = panelCurrentState.horizontalSlots + 1
                                    )
                                    viewModel.updateEditingPanel(newPanelState)
                                }
                            }) {
                                Text("+")
                            }
                            Button({
                                style {
                                    gridColumn("${panelCurrentState.verticalSlots + 1} / ${panelCurrentState.verticalSlots + 1}")
                                    gridRow("${1} / ${panelCurrentState.horizontalSlots + 1}")
                                }
                                classes("btn", "btn-primary")
                                onClick {
                                    val newPanelState = panelCurrentState.copy(
                                        verticalSlots = panelCurrentState.verticalSlots + 1
                                    )
                                    viewModel.updateEditingPanel(newPanelState)
                                }
                            }) {
                                Text("+")
                            }
                            if (draggingIndex.value != null) {
                                Div({
                                    classes(PanelViewStylesheet.panelViewGridTrashArea, "align-middle")
                                    style {
                                        gridColumn("${panelCurrentState.verticalSlots + 1} / ${panelCurrentState.verticalSlots + 3}")
                                        gridRow("${panelCurrentState.horizontalSlots + 1} / ${panelCurrentState.horizontalSlots + 3}")
                                    }
                                    onDragOver {
                                        it.preventDefault()
                                    }

                                    onDrop {
                                        it.preventDefault()
                                        val draggingIndexValue = draggingIndex.value
                                        if (draggingIndexValue != null && resizingParameters.value == null) {
                                            val newPanel = panelCurrentState.copy(
                                                items = panelCurrentState.items.toMutableList().run {
                                                    removeAt(draggingIndexValue)
                                                    toList()
                                                }
                                            )
                                            viewModel.updateEditingPanel(newPanel)
                                            draggingIndex.value = null
                                        }
                                    }
                                }) {
                                    Text(PanelStrings.removeCell.translation())
                                }
                            }
                        }
                    }
                }
                Div({
                    classes("btn-group", PanelViewStylesheet.managementButtonsContainer)
                    attr("role", "group")
                }) {
                    if (editMode) {
                        Button({
                            type(ButtonType.Button)
                            classes("btn", "btn-outline-primary")

                            onClick {
                                val config = viewModel.exportCurrentConfig()
                                val configBlob = Blob(arrayOf(config), BlobPropertyBag(type = "text/plain"))
                                val fileUrl = URL.createObjectURL(configBlob)
                                try {
                                    triggerDownloadFile(
                                        "config.json",
                                        fileUrl
                                    )
                                } finally {
                                    URL.revokeObjectURL(fileUrl)
                                }
                            }
                        }) {
                            Text(PanelStrings.exportPanel.translation())
                        }
                        Button({
                            type(ButtonType.Button)
                            classes("btn", "btn-outline-primary")

                            onClick {
                                selectFile {
                                    it
                                        .readBytesPromise()
                                        .then {
                                            it.decodeToString()
                                        }
                                        .then {
                                            viewModel.importConfig(it)
                                        }
                                }
                            }
                        }) {
                            Text(PanelStrings.importPanel.translation())
                        }
                    }
                    Button({
                        type(ButtonType.Button)
                        classes("btn", "btn-outline-primary")
                        if (editMode) {
                            classes("active")
                        }

                        onClick {
                            if (editMode) {
                                viewModel.onCancelEditing()
                            } else {
                                viewModel.onEnterEditing()
                            }
                        }
                    }) {
                        Text(PanelStrings.editPanel.translation())
                    }
                    if (editMode) {
                        Button({
                            type(ButtonType.Button)
                            classes("btn", )
                            if (panelCurrentState == panelStateValue) {
                                classes("disabled", "btn-outline-success")
                                disabled()
                            } else {
                                classes("btn-success")
                            }

                            onClick {
                                viewModel.savePanel()
                            }
                        }) {
                            Text(PanelStrings.save.translation())
                        }
                    }
                }
                val newPanelViewConfigXY = viewModel.newPaneViewConfigXY.collectAsState()
                if (editMode && newPanelViewConfigXY.value != null) {
                    ConfigEditingModal()
                }
            }
        }
    }

    /** Constants associated with the panel view's add-item dialog. */
    companion object {
        /** Conventional add-item modal identifier exposed to callers. */
        const val addConfigModalId = "panel_view_add_config_modal"
    }
}

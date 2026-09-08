package space.kscience.frameswork.features.common.web.ui

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.disabled
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.Main
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

data class FlotViewMetric(
    val label: String,
    val value: String,
    val unit: String? = null,
    val tone: FlotViewTone = FlotViewTone.Neutral,
    val hint: String? = null,
)

data class FlotViewTab(
    val label: String,
    val selected: Boolean = false,
    val disabled: Boolean = false,
)

data class FlotViewSetpoint(
    val label: String,
    val actual: String,
    val target: String,
    val unit: String,
    val plan: String? = null,
    val step: String? = null,
    val limits: String? = null,
    val pending: Boolean = false,
    val disabled: Boolean = false,
    val decrementDisabled: Boolean = false,
    val incrementDisabled: Boolean = false,
    val message: String? = null,
    val messageTone: FlotViewTone = FlotViewTone.Warning,
    val actualLabel: String = "actual",
    val stepLabel: String = "step",
    val limitsLabel: String = "limits",
)

enum class FlotViewMode { Auto, Manual }

sealed interface FlotViewSendState {
    data object Idle : FlotViewSendState
    data class Pending(val changes: Int) : FlotViewSendState
    data object Sending : FlotViewSendState
    data class Blocked(val message: String) : FlotViewSendState
}

sealed interface FlotViewState<out T> {
    data object Loading : FlotViewState<Nothing>
    data object Empty : FlotViewState<Nothing>
    data class Error(val message: String) : FlotViewState<Nothing>
    data class Ready<T>(val value: T) : FlotViewState<T>
}

/**
 * Top-level 1280×800 panel shell from the reference design.
 *
 * [content] is placed in the responsive two-column body. Use [FlotViewColumn] for each column.
 * Styles are installed automatically, so a panel is usable without a separate setup call.
 */
@Composable
fun FlotViewPanel(
    title: String,
    subtitle: String? = null,
    headerContent: (@Composable () -> Unit)?,
    content: (@Composable () -> Unit)?,
) {
    Main(attrs = { classes(FlotViewStyleSheet.rootClass) }) {
        Div(attrs = { classes(FlotViewStyleSheet.panelClass) }) {
            Div(attrs = { classes(FlotViewStyleSheet.headerClass) }) {
                Div(attrs = { classes(FlotViewStyleSheet.headerContextClass) }) {
                    H1(attrs = { classes(FlotViewStyleSheet.titleClass) }) { Text(title) }
                    subtitle?.let {
                        P(attrs = { classes(FlotViewStyleSheet.subtitleClass) }) { Text(it) }
                    }
                }
                headerContent ?.let {
                    Div(attrs = { classes(FlotViewStyleSheet.headerActionsClass) }) {
                        headerContent()
                    }
                }
            }
            content ?.let {
                Div(attrs = { classes(FlotViewStyleSheet.bodyClass) }) {
                    content()
                }
            }
        }
    }
}

/** A body column. Pass [controls] for the right-hand controls column. */
@Composable
fun FlotViewColumn(
    controls: Boolean = false,
    content: @Composable () -> Unit,
) {
    Div(attrs = {
        classes(
            FlotViewStyleSheet.columnClass,
            if (controls) {
                FlotViewStyleSheet.controlsColumnClass
            } else {
                FlotViewStyleSheet.primaryColumnClass
            },
        )
    }) {
        content()
    }
}

@Composable
fun FlotViewCard(
    title: String? = null,
    raised: Boolean = false,
    content: @Composable () -> Unit,
) {
    Div(attrs = {
        classes(FlotViewStyleSheet.cardClass)
        if (raised) classes(FlotViewStyleSheet.raisedCardClass)
    }) {
        title?.let {
            Div(attrs = { classes(FlotViewStyleSheet.sectionTitleClass) }) { Text(it) }
        }
        content()
    }
}

/** Dark process/video surface. Its child controls the actual image or canvas rendering. */
@Composable
fun FlotViewVisualSurface(content: @Composable () -> Unit) {
    Div(attrs = { classes(FlotViewStyleSheet.visualClass) }) {
        content()
    }
}

/** Overlay for loss-of-signal or another video-layer state. */
@Composable
fun FlotViewVisualOverlay(
    title: String,
    detail: String? = null,
    eyebrow: String? = null,
    tone: FlotViewTone = FlotViewTone.Error,
) {
    Div(attrs = { classes(FlotViewStyleSheet.visualOverlayClass) }) {
        FlotViewStatePanel(
            title = title,
            detail = detail,
            eyebrow = eyebrow,
            tone = tone,
            dark = true,
        )
    }
}

/** Small tone-bearing status element intended for the panel header. */
@Composable
fun FlotViewStatusChip(
    tone: FlotViewTone = FlotViewTone.Neutral,
    showIndicator: Boolean = true,
    content: @Composable () -> Unit,
) {
    Div(attrs = {
        classes(FlotViewStyleSheet.statusChipClass, tone.cssClass)
        attr("role", if (tone == FlotViewTone.Error) "alert" else "status")
    }) {
        if (showIndicator) {
            Span(attrs = {
                classes(FlotViewStyleSheet.statusDotClass)
                attr("aria-hidden", "true")
            })
        }
        content()
    }
}

@Composable
fun FlotViewMetricGrid(metrics: List<FlotViewMetric>) {
    Div(attrs = { classes(FlotViewStyleSheet.metricGridClass) }) {
        metrics.forEach { metric ->
            FlotViewMetricCard(
                value = { Text(metric.value) },
                unit = metric.unit?.let { unit -> { Text(unit) } },
                tone = metric.tone,
                hint = metric.hint?.let { hint -> { Text(hint) } },
                label = { Text(metric.label) },
            )
        }
    }
}

@Composable
fun FlotViewMetricCardContainer(
    tone: FlotViewTone = FlotViewTone.Neutral,
    content: @Composable () -> Unit,
) {
    Div(attrs = {
        classes(FlotViewStyleSheet.metricClass, tone.cssClass)
    }) {
        content()
    }
}

@Composable
fun FlotViewMetricCardLabel(
    label: @Composable () -> Unit,
) {
    Div(attrs = { classes(FlotViewStyleSheet.metricLabelClass) }) {
        label()
    }
}

@Composable
fun FlotViewMetricCard(
    value: @Composable () -> Unit,
    unit: (@Composable () -> Unit)? = null,
    tone: FlotViewTone = FlotViewTone.Neutral,
    hint: (@Composable () -> Unit)? = null,
    label: @Composable () -> Unit,
) {
    FlotViewMetricCardContainer(tone) {
        FlotViewMetricCardLabel(label)
        Div(attrs = { classes(FlotViewStyleSheet.metricValueClass) }) {
            Span { value() }
            unit?.let {
                Span(attrs = { classes(FlotViewStyleSheet.metricUnitClass) }) { it() }
            }
        }
        hint?.let {
            Div(attrs = { classes(FlotViewStyleSheet.metricHintClass) }) { it() }
        }
    }
}

@Composable
fun FlotViewMetricCardUnspannedValue(
    value: @Composable () -> Unit,
    unit: (@Composable () -> Unit)? = null,
    tone: FlotViewTone = FlotViewTone.Neutral,
    hint: (@Composable () -> Unit)? = null,
    label: @Composable () -> Unit,
) {
    FlotViewMetricCardContainer(tone) {
        Div(attrs = { classes(FlotViewStyleSheet.metricLabelClass) }) {
            label()
        }
        Div(attrs = { classes(FlotViewStyleSheet.metricValueClass) }) {
            value()
            unit?.let {
                Span(attrs = { classes(FlotViewStyleSheet.metricUnitClass) }) { it() }
            }
        }
        hint?.let {
            Div(attrs = { classes(FlotViewStyleSheet.metricHintClass) }) { it() }
        }
    }
}

@Composable
fun FlotViewTabs(
    tabs: List<FlotViewTab>,
    onSelected: (index: Int) -> Unit,
    label: String? = null,
) {
    Div(attrs = {
        classes(FlotViewStyleSheet.tabsClass)
        attr("role", "tablist")
        label?.let { attr("aria-label", it) }
    }) {
        label?.let {
            Div(attrs = { classes(FlotViewStyleSheet.tabsLabelClass) }) { Text(it) }
        }
        tabs.forEachIndexed { index, item ->
            Button(attrs = {
                classes(FlotViewStyleSheet.tabClass)
                if (item.selected) classes(FlotViewStyleSheet.activeTabClass)
                type(ButtonType.Button)
                attr("role", "tab")
                attr("aria-selected", item.selected.toString())
                if (item.disabled) disabled()
                if (!item.disabled) onClick { onSelected(index) }
            }) {
                Text(item.label)
            }
        }
    }
}

/** Compact status/recommendation banner for a single message. */
@Composable
fun FlotViewStatusBanner(
    message: String,
    tone: FlotViewTone = FlotViewTone.Neutral,
) {
    Div(attrs = {
        classes(FlotViewStyleSheet.bannerClass, tone.cssClass)
        attr("role", if (tone == FlotViewTone.Error) "alert" else "status")
    }) {
        Text(message)
    }
}

/** Full recommendation card, including explicit accept and dismiss actions. */
@Composable
fun FlotViewRecommendationCard(
    title: @Composable () -> Unit,
    description: @Composable () -> Unit,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    eyebrow: String = "Recommendation",
    confidence: String? = null,
    tone: FlotViewTone = FlotViewTone.Info,
    acceptLabel: String = "Apply recommendation",
    dismissLabel: String = "Dismiss",
    actionsDisabled: Boolean = false,
) {
    Div(attrs = {
        classes(FlotViewStyleSheet.bannerClass, tone.cssClass)
        attr("role", "status")
    }) {
        Div(attrs = { classes(FlotViewStyleSheet.bannerHeaderClass) }) {
            Div(attrs = { classes(FlotViewStyleSheet.bannerEyebrowClass) }) {
                Text(eyebrow)
            }
            confidence?.let {
                Div(attrs = { classes(FlotViewStyleSheet.bannerConfidenceClass) }) { Text(it) }
            }
        }
        H2(attrs = { classes(FlotViewStyleSheet.bannerTitleClass) }) {
            title()
        }
        P(attrs = { classes(FlotViewStyleSheet.bannerBodyClass) }) {
            description()
        }
        Div(attrs = { classes(FlotViewStyleSheet.bannerActionsClass) }) {
            FlotViewActionButton(
                label = acceptLabel,
                onClick = onAccept,
                primary = true,
                isDisabled = actionsDisabled,
            )
            FlotViewActionButton(
                label = dismissLabel,
                onClick = onDismiss,
                isDisabled = actionsDisabled,
            )
        }
    }
}

@Composable
fun FlotViewModeSwitch(
    selected: FlotViewMode,
    onSelected: (FlotViewMode) -> Unit,
    enabled: Boolean = true,
    label: String = "Control mode",
) {
    Div(attrs = {
        classes(FlotViewStyleSheet.modeClass)
        attr("role", "group")
        attr("aria-label", label)
    }) {
        FlotViewMode.entries.forEach { option ->
            Button(attrs = {
                classes(FlotViewStyleSheet.modeOptionClass)
                if (option == selected) classes(FlotViewStyleSheet.modeActiveClass)
                type(ButtonType.Button)
                attr("aria-pressed", (option == selected).toString())
                if (!enabled) disabled()
                if (enabled) onClick { onSelected(option) }
            }) {
                Text(option.name.uppercase())
            }
        }
    }
}

@Composable
fun FlotViewStepper(
    value: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    decrementDisabled: Boolean = false,
    incrementDisabled: Boolean = false,
    unit: String? = null,
    label: String? = null,
    decrementLabel: String = "Decrease",
    incrementLabel: String = "Increase",
) {
    Div(attrs = {
        classes(FlotViewStyleSheet.stepperClass)
        attr("role", "group")
        label?.let { attr("aria-label", it) }
    }) {
        Button(attrs = {
            classes(FlotViewStyleSheet.stepperButtonClass)
            type(ButtonType.Button)
            attr("aria-label", decrementLabel)
            if (decrementDisabled) disabled()
            if (!decrementDisabled) onClick { onDecrement() }
        }) {
            Text("−")
        }
        Div(attrs = {
            classes(FlotViewStyleSheet.stepperValueClass)
            attr("aria-live", "polite")
        }) {
            Span(attrs = { classes(FlotViewStyleSheet.stepperNumberClass) }) { Text(value) }
            unit?.let {
                Span(attrs = { classes(FlotViewStyleSheet.stepperUnitClass) }) { Text(it) }
            }
        }
        Button(attrs = {
            classes(FlotViewStyleSheet.stepperButtonClass)
            type(ButtonType.Button)
            attr("aria-label", incrementLabel)
            if (incrementDisabled) disabled()
            if (!incrementDisabled) onClick { onIncrement() }
        }) {
            Text("+")
        }
    }
}

@Composable
fun FlotViewSetpointRow(
    pending: Boolean = false,
    disabled: Boolean = false,
    messageTone: FlotViewTone = FlotViewTone.Warning,
    message: (@Composable () -> Unit)? = null,
    stepper: @Composable () -> Unit,
    setpoint: @Composable () -> Unit,
) {
    Div(attrs = {
        classes(FlotViewStyleSheet.setpointRowClass)
        if (pending) classes(FlotViewStyleSheet.setpointPendingClass)
        if (disabled) classes(FlotViewStyleSheet.setpointDisabledClass)
    }) {
        Div(attrs = { classes(FlotViewStyleSheet.setpointTextClass) }) {
            setpoint()
        }
        stepper()
        message?.let {
            Div(attrs = {
                classes(FlotViewStyleSheet.setpointMessageClass, messageTone.cssClass)
                attr("role", if (messageTone == FlotViewTone.Error) "alert" else "status")
            }) {
                it()
            }
        }
    }
}

@Composable
fun FlotViewSetpointList(content: @Composable () -> Unit) {
    Div(attrs = { classes(FlotViewStyleSheet.setpointListClass) }) {
        content()
    }
}

@Composable
fun FlotViewActionButton(
    label: String,
    onClick: () -> Unit,
    primary: Boolean = false,
    danger: Boolean = false,
    isDisabled: Boolean = false,
    success: Boolean = false,
    extraClass: String? = null,
) {
    val variant = when {
        danger -> FlotViewStyleSheet.actionDangerClass
        success -> FlotViewStyleSheet.actionSuccessClass
        primary -> FlotViewStyleSheet.actionPrimaryClass
        else -> FlotViewStyleSheet.actionNeutralClass
    }
    Button(attrs = {
        classes(FlotViewStyleSheet.actionClass, variant)
        extraClass?.let { classes(it) }
        type(ButtonType.Button)
        if (isDisabled) disabled()
        if (!isDisabled) onClick { onClick() }
    }) {
        Text(label)
    }
}

/**
 * APC send row with the four states in the design specification: idle, pending, sending, and
 * communication-blocked.
 */
@Composable
fun FlotViewSendControls(
    state: FlotViewSendState,
    onSend: () -> Unit,
    sendLabel: String = "Send setpoints to APC",
    idleMessage: String = "No unsaved changes",
    sendingLabel: String = "Sending…",
    sendingMessage: String = "Waiting for APC confirmation",
    pendingMessage: (changes: Int) -> String = { "$it changes waiting to be sent" },
) {
    val buttonLabel = if (state == FlotViewSendState.Sending) sendingLabel else sendLabel
    val message = when (state) {
        FlotViewSendState.Idle -> idleMessage
        is FlotViewSendState.Pending -> pendingMessage(state.changes)
        FlotViewSendState.Sending -> sendingMessage
        is FlotViewSendState.Blocked -> state.message
    }
    val disabled = state !is FlotViewSendState.Pending

    Div(attrs = { classes(FlotViewStyleSheet.sendRowClass) }) {
        FlotViewActionButton(
            label = buttonLabel,
            onClick = onSend,
            success = state is FlotViewSendState.Pending,
            isDisabled = disabled,
            extraClass = FlotViewStyleSheet.sendButtonClass,
        )
        Div(attrs = {
            classes(FlotViewStyleSheet.sendMessageClass)
            when (state) {
                is FlotViewSendState.Pending -> classes(FlotViewStyleSheet.sendPendingClass)
                is FlotViewSendState.Blocked -> classes(FlotViewStyleSheet.sendErrorClass)
                else -> Unit
            }
            attr("role", if (state is FlotViewSendState.Blocked) "alert" else "status")
            attr("aria-live", "polite")
        }) {
            Text(message)
        }
    }
}

@Composable
fun FlotViewStatePanel(
    title: String,
    detail: String? = null,
    eyebrow: String? = null,
    tone: FlotViewTone = FlotViewTone.Neutral,
    dark: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Div(attrs = {
        classes(FlotViewStyleSheet.stateClass, tone.cssClass)
        when (tone) {
            FlotViewTone.Info -> classes(FlotViewStyleSheet.stateLoadingClass)
            FlotViewTone.Error -> classes(FlotViewStyleSheet.stateErrorClass)
            FlotViewTone.Neutral -> classes(FlotViewStyleSheet.stateEmptyClass)
            else -> Unit
        }
        if (dark) classes(FlotViewStyleSheet.stateDarkClass)
        attr("role", if (tone == FlotViewTone.Error) "alert" else "status")
    }) {
        eyebrow?.let {
            Div(attrs = { classes(FlotViewStyleSheet.stateEyebrowClass) }) { Text(it) }
        }
        Div(attrs = { classes(FlotViewStyleSheet.stateTitleClass) }) { Text(title) }
        detail?.let {
            P(attrs = { classes(FlotViewStyleSheet.stateDetailClass) }) { Text(it) }
        }
        if (actionLabel != null && onAction != null) {
            FlotViewActionButton(
                label = actionLabel,
                onClick = onAction,
                primary = true,
            )
        }
    }
}

@Composable
fun <T> FlotViewStateContent(
    state: FlotViewState<T>,
    onRetry: (() -> Unit)? = null,
    loadingMessage: String = "Loading…",
    emptyMessage: String = "No data available",
    retryLabel: String = "Retry",
    content: @Composable (T) -> Unit,
) {
    when (state) {
        FlotViewState.Loading -> FlotViewStatePanel(
            title = loadingMessage,
            tone = FlotViewTone.Info,
        )

        FlotViewState.Empty -> FlotViewStatePanel(
            title = emptyMessage,
            tone = FlotViewTone.Neutral,
        )

        is FlotViewState.Error -> FlotViewStatePanel(
            title = state.message,
            tone = FlotViewTone.Error,
            actionLabel = if (onRetry != null) retryLabel else null,
            onAction = onRetry,
        )

        is FlotViewState.Ready -> content(state.value)
    }
}

private val FlotViewTone.cssClass: String
    get() = when (this) {
        FlotViewTone.Neutral -> FlotViewStyleSheet.toneNeutralClass
        FlotViewTone.Info -> FlotViewStyleSheet.toneInfoClass
        FlotViewTone.Success -> FlotViewStyleSheet.toneSuccessClass
        FlotViewTone.Warning -> FlotViewStyleSheet.toneWarningClass
        FlotViewTone.Error -> FlotViewStyleSheet.toneErrorClass
    }

internal fun formatFlotViewMeasurement(value: String, unit: String): String = when (unit) {
    "%", "°", "°C", "°F" -> "$value$unit"
    "" -> value
    else -> "$value $unit"
}

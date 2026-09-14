package space.kscience.frameswork.features.common.web.ui

import androidx.compose.runtime.Composable
import dev.inmo.micro_utils.coroutines.compose.StyleSheetsAggregator
import org.jetbrains.compose.web.css.CSSColorValue
import org.jetbrains.compose.web.css.Color
import org.jetbrains.compose.web.css.Style
import org.jetbrains.compose.web.css.StyleSheet
import org.jetbrains.compose.web.css.media

/**
 * ISA-101 inspired design tokens from the FlotView panel specification.
 *
 * Neutral colours carry the normal operating state. Saturated colours are reserved for
 * information, a deviation, a pending action, or an alarm.
 */
object FlotViewTokens {
    val canvas = Color("#E7E7E4")
    val surface = Color("#F4F4F2")
    val raised = Color("#FFFFFF")
    val ink = Color("#1A1A19")
    val muted = Color("#6C6C69")
    val line = Color("#C9C9C5")
    val track = Color("#D8D8D4")
    val alarm = Color("#C0392B")
    val warning = Color("#C77D0A")
    val info = Color("#2C6E9B")
    val ok = Color("#3C7D5A")
    val pendingBackground = Color("#FBF4E7")
    val process = Color("#292B28")
    val processGrid = Color("#4B4D48")

    // Compatibility aliases for callers that used the first draft of this UI kit.
    val border = line
    val borderStrong = line
    val disabled = track
    val blue = info
    val green = ok
    val amber = warning
    val red = alarm
    val map = process
    val mapGrid = processGrid
    val quiet = line

    const val unit = 8
    const val screenPadding = 12
    const val headerHeight = 64
    const val stepperButton = 72
    const val minimumTouchTarget = 83
    const val comfortableTouchTarget = 110
    const val recommendationButtonHeight = 56
    const val modeButtonHeight = 44
    const val sendButtonHeight = 64

    // Kept for source compatibility with the original draft.
    const val touchTarget = minimumTouchTarget
}

/**
 * Semantic visual tone used by FlotView components.
 *
 * @property value CSS colour associated with the tone.
 */
enum class FlotViewTone(val value: CSSColorValue) {
    /** Normal or secondary information. */
    Neutral(FlotViewTokens.muted),

    /** Informational state that calls for attention without warning. */
    Info(FlotViewTokens.info),

    /** Successful, healthy, or authorized state. */
    Success(FlotViewTokens.ok),

    /** Deviation or pending action requiring operator attention. */
    Warning(FlotViewTokens.warning),

    /** Alarm, failed operation, or unsafe state. */
    Error(FlotViewTokens.alarm),
}

/**
 * Styles shared by all FlotView Compose Web components.
 *
 * Public `*Class` constants are stable hooks used by the rendering components and may also be used
 * by host markup. Constants without the `Class` suffix are compatibility aliases retained for the
 * original UI-kit API.
 */
object FlotViewStyleSheet : StyleSheet() {
    const val rootClass = "flotview-root"
    const val panelClass = "flotview-panel"
    const val headerClass = "flotview-header"
    const val headerContextClass = "flotview-header-context"
    const val headerActionsClass = "flotview-header-actions"
    const val titleClass = "flotview-title"
    const val subtitleClass = "flotview-subtitle"
    const val bodyClass = "flotview-body"
    const val columnClass = "flotview-column"
    const val primaryColumnClass = "flotview-column-primary"
    const val controlsColumnClass = "flotview-column-controls"
    const val cardClass = "flotview-card"
    const val raisedCardClass = "flotview-card-raised"
    const val sectionTitleClass = "flotview-section-title"
    const val statusChipClass = "flotview-status-chip"
    const val statusDotClass = "flotview-status-dot"
    const val visualClass = "flotview-visual"
    const val visualOverlayClass = "flotview-visual-overlay"

    const val metricGridClass = "flotview-metric-grid"
    const val metricClass = "flotview-metric"
    const val metricLabelClass = "flotview-metric-label"
    const val metricValueClass = "flotview-metric-value"
    const val metricUnitClass = "flotview-metric-unit"
    const val metricHintClass = "flotview-metric-hint"

    const val tabsClass = "flotview-tabs"
    const val tabsLabelClass = "flotview-tabs-label"
    const val tabClass = "flotview-tab"
    const val activeTabClass = "flotview-tab-active"

    const val bannerClass = "flotview-banner"
    const val bannerHeaderClass = "flotview-banner-header"
    const val bannerEyebrowClass = "flotview-banner-eyebrow"
    const val bannerConfidenceClass = "flotview-banner-confidence"
    const val bannerTitleClass = "flotview-banner-title"
    const val bannerBodyClass = "flotview-banner-body"
    const val bannerActionsClass = "flotview-banner-actions"

    const val modeClass = "flotview-mode"
    const val modeOptionClass = "flotview-mode-option"
    const val modeActiveClass = "flotview-mode-active"

    const val setpointListClass = "flotview-setpoint-list"
    const val setpointRowClass = "flotview-setpoint-row"
    const val setpointPendingClass = "flotview-setpoint-pending"
    const val setpointDisabledClass = "flotview-setpoint-disabled"
    const val setpointTextClass = "flotview-setpoint-text"
    const val setpointLabelClass = "flotview-setpoint-label"
    const val setpointDetailsClass = "flotview-setpoint-details"
    const val setpointPlanClass = "flotview-setpoint-plan"
    const val setpointMessageClass = "flotview-setpoint-message"

    const val stepperClass = "flotview-stepper"
    const val stepperButtonClass = "flotview-stepper-button"
    const val stepperValueClass = "flotview-stepper-value"
    const val stepperNumberClass = "flotview-stepper-number"
    const val stepperUnitClass = "flotview-stepper-unit"

    const val actionClass = "flotview-action"
    const val actionPrimaryClass = "flotview-action-primary"
    const val actionNeutralClass = "flotview-action-neutral"
    const val actionSuccessClass = "flotview-action-success"
    const val actionDangerClass = "flotview-action-danger"

    const val sendRowClass = "flotview-send-row"
    const val sendButtonClass = "flotview-send-button"
    const val sendMessageClass = "flotview-send-message"
    const val sendPendingClass = "flotview-send-pending"
    const val sendErrorClass = "flotview-send-error"

    const val stateClass = "flotview-state"
    const val stateLoadingClass = "flotview-state-loading"
    const val stateErrorClass = "flotview-state-error"
    const val stateEmptyClass = "flotview-state-empty"
    const val stateDarkClass = "flotview-state-dark"
    const val stateEyebrowClass = "flotview-state-eyebrow"
    const val stateTitleClass = "flotview-state-title"
    const val stateDetailClass = "flotview-state-detail"

    const val dashboardCellButtonClass = "flotview-dashboard-cell-button"
    const val dashboardHeaderStatusClass = "flotview-dashboard-header-status"
    const val dashboardOperatorClass = "flotview-dashboard-operator"
    const val dashboardOperatorIdentityClass = "flotview-dashboard-operator-identity"
    const val dashboardOperatorNameClass = "flotview-dashboard-operator-name"
    const val dashboardOperatorDetailsClass = "flotview-dashboard-operator-details"
    const val dashboardTimeClass = "flotview-dashboard-time"
    const val dashboardVideoContentClass = "flotview-dashboard-video-content"
    const val dashboardVideoPlaceholderClass = "flotview-dashboard-video-placeholder"
    const val dashboardVideoBadgeClass = "flotview-dashboard-video-badge"
    const val dashboardVideoBadgeTopLeftClass = "flotview-dashboard-video-badge-top-left"
    const val dashboardVideoBadgeTopRightClass = "flotview-dashboard-video-badge-top-right"
    const val dashboardVideoBadgeBottomLeftClass = "flotview-dashboard-video-badge-bottom-left"
    const val dashboardVideoBadgeBottomRightClass = "flotview-dashboard-video-badge-bottom-right"
    const val dashboardAiUnavailableClass = "flotview-dashboard-ai-unavailable"
    const val dashboardAiUnavailableTitleClass = "flotview-dashboard-ai-unavailable-title"
    const val dashboardAiUnavailableDetailClass = "flotview-dashboard-ai-unavailable-detail"
    const val dashboardControlsCardClass = "flotview-dashboard-controls-card"
    const val dashboardControlsHeaderClass = "flotview-dashboard-controls-header"
    const val dashboardControlsTitleClass = "flotview-dashboard-controls-title"
    const val dashboardPolicyClass = "flotview-dashboard-policy"
    const val dashboardSendDockClass = "flotview-dashboard-send-dock"
    const val dashboardDistributionClass = "flotview-dashboard-distribution"
    const val dashboardDistributionHeaderClass = "flotview-dashboard-distribution-header"
    const val dashboardDistributionSummaryClass = "flotview-dashboard-distribution-summary"
    const val dashboardDistributionTrackClass = "flotview-dashboard-distribution-track"
    const val dashboardDistributionSegmentClass = "flotview-dashboard-distribution-segment"
    const val dashboardDistributionLegendClass = "flotview-dashboard-distribution-legend"
    const val dashboardDistributionLegendItemClass = "flotview-dashboard-distribution-legend-item"
    const val dashboardDistributionSwatchClass = "flotview-dashboard-distribution-swatch"
    const val dashboardLockedClass = "flotview-dashboard-locked"

    const val toneNeutralClass = "flotview-tone-neutral"
    const val toneInfoClass = "flotview-tone-info"
    const val toneSuccessClass = "flotview-tone-success"
    const val toneWarningClass = "flotview-tone-warning"
    const val toneErrorClass = "flotview-tone-error"

    // Compatibility aliases for the names exposed by the original draft.
    const val panel = panelClass
    const val header = headerClass
    const val title = titleClass
    const val subtitle = subtitleClass
    const val card = cardClass
    const val metricGrid = metricGridClass
    const val metric = metricClass
    const val metricLabel = metricLabelClass
    const val metricValue = metricValueClass
    const val metricUnit = metricUnitClass
    const val tabs = tabsClass
    const val tab = tabClass
    const val activeTab = activeTabClass
    const val banner = bannerClass
    const val mode = modeClass
    const val modeOption = modeOptionClass
    const val modeActive = modeActiveClass
    const val stepper = stepperClass
    const val stepperButton = stepperButtonClass
    const val stepperValue = stepperValueClass
    const val action = actionClass
    const val actionPrimary = actionPrimaryClass
    const val actionMuted = actionNeutralClass
    const val actionDanger = actionDangerClass
    const val state = stateClass
    const val stateLoading = stateLoadingClass
    const val stateError = stateErrorClass
    const val stateEmpty = stateEmptyClass

    init {
        ".$rootClass, .$rootClass *, [class*=\"flotview-\"], [class*=\"flotview-\"] *" {
            property("box-sizing", "border-box")
        }

        ".$rootClass" {
            property("align-items", "stretch")
            property("background-color", FlotViewTokens.canvas)
            property("color", FlotViewTokens.ink)
            property("display", "flex")
            property("flex-direction", "column")
            property("font-family", "\"IBM Plex Sans\", Inter, Arial, sans-serif")
            property("font-size", "14px")
            property("height", "100%")
            property("line-height", "1.35")
            property("min-height", "0")
            property("min-width", "0")
            property("overflow", "auto")
            property("width", "100%")
        }

        ".$rootClass button" {
            property("border-radius", "0")
            property("font", "inherit")
        }

        ".$rootClass button:focus-visible" {
            property("outline", "2px solid ${FlotViewTokens.info}")
            property("outline-offset", "2px")
        }

        ".$rootClass button:disabled" {
            property("cursor", "not-allowed")
        }

        ".$panelClass" {
            property("display", "flex")
            property("flex", "1 1 auto")
            property("flex-direction", "column")
            property("gap", "8px")
            property("height", "100%")
            property("margin", "0")
            property("max-width", "none")
            property("min-height", "0")
            property("min-width", "0")
            property("padding", "12px")
            property("width", "100%")
        }

        ".$headerClass" {
            property("align-items", "center")
            property("background-color", FlotViewTokens.raised)
            property("border", "1px solid ${FlotViewTokens.line}")
            property("display", "flex")
            property("flex", "0 0 auto")
            property("gap", "12px")
            property("justify-content", "space-between")
            property("min-width", "0")
            property("min-height", "64px")
            property("padding", "8px 12px")
            property("width", "100%")
        }

        ".$headerContextClass" {
            property("flex", "0 1 auto")
            property("max-width", "100%")
            property("min-width", "0")
        }

        ".$headerActionsClass" {
            property("align-items", "center")
            property("display", "flex")
            property("flex", "1")
            property("flex-wrap", "wrap")
            property("gap", "8px")
            property("justify-content", "flex-end")
            property("max-width", "100%")
            property("min-width", "0")
            property("width", "100%")
        }

        ".$titleClass" {
            property("font-size", "20px")
            property("font-weight", "700")
            property("letter-spacing", "-0.01em")
            property("line-height", "1.15")
            property("margin", "0")
        }

        ".$subtitleClass" {
            property("color", FlotViewTokens.muted)
            property("font-size", "13px")
            property("margin", "2px 0 0")
        }

        ".$bodyClass" {
            property("align-items", "stretch")
            property("display", "grid")
            property("flex", "1")
            property("gap", "8px")
            property("grid-template-columns", "minmax(0, 1.32fr) minmax(360px, 1fr)")
            property("grid-template-rows", "minmax(0, 1fr)")
            property("height", "100%")
            property("min-height", "0")
            property("min-width", "0")
            property("overflow", "auto")
            property("width", "100%")
        }

        ".$columnClass" {
            property("align-self", "stretch")
            property("display", "flex")
            property("flex", "1 1 auto")
            property("flex-direction", "column")
            property("gap", "8px")
            property("height", "100%")
            property("max-width", "100%")
            property("min-height", "0")
            property("min-width", "0")
            property("width", "100%")
        }

        ".$columnClass > *" {
            property("max-width", "100%")
            property("min-width", "0")
            property("width", "100%")
        }

        ".$columnClass > :only-child" {
            property("flex", "1 1 auto")
            property("height", "100%")
            property("min-height", "0")
        }

        ".$columnClass > .$bannerClass:only-child .$bannerActionsClass" {
            property("margin-top", "auto")
            property("padding-top", "12px")
        }

        ".$primaryColumnClass" {
            property("grid-column", "1")
        }

        ".$controlsColumnClass" {
            property("grid-column", "2")
        }

        ".$cardClass" {
            property("background-color", FlotViewTokens.surface)
            property("border", "1px solid ${FlotViewTokens.line}")
            property("max-width", "100%")
            property("min-width", "0")
            property("padding", "12px")
            property("width", "100%")
        }

        ".$raisedCardClass" {
            property("background-color", FlotViewTokens.raised)
        }

        ".$sectionTitleClass, .$metricLabelClass, .$tabsLabelClass, .$stateEyebrowClass" {
            property("color", FlotViewTokens.muted)
            property("font-size", "11px")
            property("font-weight", "600")
            property("letter-spacing", "0.05em")
            property("text-transform", "uppercase")
        }

        ".$sectionTitleClass" {
            property("margin", "0 0 8px")
        }

        ".$statusChipClass" {
            property("align-items", "center")
            property("background-color", FlotViewTokens.raised)
            property("border", "1px solid var(--flotview-tone)")
            property("color", "var(--flotview-tone)")
            property("display", "inline-flex")
            property("font-size", "13px")
            property("font-weight", "600")
            property("gap", "8px")
            property("max-width", "100%")
            property("min-height", "36px")
            property("min-width", "0")
            property("padding", "6px 10px")
            property("overflow-wrap", "anywhere")
        }

        ".$statusDotClass" {
            property("background-color", "var(--flotview-tone)")
            property("border-radius", "50%")
            property("display", "inline-block")
            property("flex", "0 0 8px")
            property("height", "8px")
            property("width", "8px")
        }

        ".$visualClass" {
            property("aspect-ratio", "16 / 9")
            property("background-color", FlotViewTokens.process)
            property("border", "1px solid ${FlotViewTokens.line}")
            property("color", FlotViewTokens.raised)
            property("display", "grid")
            property("flex", "1 1 auto")
            property("height", "100%")
            property("isolation", "isolate")
            property("max-height", "100%")
            property("max-width", "100%")
            property("min-height", "0")
            property("min-width", "0")
            property("overflow", "hidden")
            property("place-items", "stretch")
            property("position", "relative")
            property("width", "100%")
        }

        ".$visualOverlayClass" {
            property("align-items", "center")
            property("background", "rgba(41, 43, 40, 0.88)")
            property("display", "flex")
            property("inset", "0")
            property("justify-content", "center")
            property("padding", "24px")
            property("position", "absolute")
            property("text-align", "center")
            property("z-index", "2")
        }

        ".$visualOverlayClass > .$stateClass" {
            property("background-color", "transparent")
            property("border", "0")
            property("width", "100%")
        }

        ".$metricGridClass" {
            property("align-items", "stretch")
            property("display", "grid")
            property("gap", "8px")
            property("grid-template-columns", "repeat(auto-fit, minmax(104px, 1fr))")
            property("max-width", "100%")
            property("min-width", "0")
            property("width", "100%")
        }

        ".$metricClass" {
            property("background-color", FlotViewTokens.raised)
            property("border", "1px solid ${FlotViewTokens.line}")
            property("display", "flex")
            property("flex", "1 1 auto")
            property("flex-direction", "column")
            property("height", "100%")
            property("max-height", "100%")
            property("max-width", "100%")
            property("min-height", "76px")
            property("min-width", "0")
            property("overflow", "hidden")
            property("padding", "8px 10px")
            property("width", "100%")
        }

        ".$metricLabelClass, .$metricValueClass, .$metricHintClass" {
            property("max-width", "100%")
            property("min-width", "0")
            property("width", "100%")
        }

        ".$metricValueClass" {
            property("align-items", "center")
            property("display", "flex")
            property("flex", "1 1 auto")
            property("font-family", "\"IBM Plex Mono\", \"SFMono-Regular\", Consolas, monospace")
            property("font-size", "26px")
            property("font-variant-numeric", "tabular-nums")
            property("font-weight", "700")
            property("line-height", "1.05")
            property("margin", "5px 0 0")
            property("white-space", "nowrap")
        }

        ".$metricUnitClass" {
            property("color", FlotViewTokens.muted)
            property("font-family", "\"IBM Plex Sans\", Inter, Arial, sans-serif")
            property("font-size", "12px")
            property("font-weight", "600")
            property("margin-left", "3px")
        }

        ".$metricHintClass" {
            property("color", FlotViewTokens.muted)
            property("font-size", "12px")
            property("margin-top", "2px")
            property("overflow", "hidden")
            property("text-overflow", "ellipsis")
            property("white-space", "nowrap")
        }

        ".$metricClass.$toneInfoClass, .$metricClass.$toneWarningClass, .$metricClass.$toneErrorClass" {
            property("border-color", "var(--flotview-tone)")
        }

        (
            ".$metricClass.$toneInfoClass .$metricValueClass, " +
                ".$metricClass.$toneSuccessClass .$metricValueClass, " +
                ".$metricClass.$toneWarningClass .$metricValueClass, " +
                ".$metricClass.$toneErrorClass .$metricValueClass"
        ) {
            property("color", "var(--flotview-tone)")
        }

        ".$tabsClass" {
            property("align-items", "center")
            property("background-color", FlotViewTokens.raised)
            property("border", "1px solid ${FlotViewTokens.line}")
            property("display", "flex")
            property("gap", "8px")
            property("max-width", "100%")
            property("min-height", "52px")
            property("min-width", "0")
            property("overflow-x", "auto")
            property("padding", "6px 8px")
            property("width", "100%")
        }

        ".$tabsLabelClass" {
            property("margin-right", "auto")
            property("white-space", "nowrap")
        }

        ".$tabClass" {
            property("background-color", FlotViewTokens.raised)
            property("border", "1px solid ${FlotViewTokens.line}")
            property("color", FlotViewTokens.muted)
            property("cursor", "pointer")
            property("font-size", "13px")
            property("font-weight", "600")
            property("min-height", "40px")
            property("min-width", "64px")
            property("padding", "9px 14px")
            property("white-space", "nowrap")
        }

        ".$tabClass:hover:not(:disabled), .$tabClass:focus-visible" {
            property("border-color", FlotViewTokens.info)
            property("color", FlotViewTokens.info)
        }

        ".$activeTabClass, .$activeTabClass:hover:not(:disabled)" {
            property("background-color", FlotViewTokens.info)
            property("border-color", FlotViewTokens.info)
            property("color", FlotViewTokens.raised)
        }

        ".$tabClass:disabled" {
            property("background-color", FlotViewTokens.track)
            property("color", FlotViewTokens.muted)
        }

        ".$bannerClass" {
            property("background-color", FlotViewTokens.raised)
            property("border", "1px solid ${FlotViewTokens.line}")
            property("border-left", "6px solid var(--flotview-tone)")
            property("display", "flex")
            property("flex-direction", "column")
            property("max-width", "100%")
            property("min-width", "0")
            property("padding", "12px 14px")
            property("width", "100%")
        }

        ".$bannerHeaderClass" {
            property("align-items", "baseline")
            property("display", "flex")
            property("gap", "12px")
            property("justify-content", "space-between")
        }

        ".$bannerEyebrowClass" {
            property("color", "var(--flotview-tone)")
            property("font-size", "12px")
            property("font-weight", "700")
            property("letter-spacing", "0.05em")
            property("text-transform", "uppercase")
        }

        ".$bannerConfidenceClass" {
            property("color", FlotViewTokens.muted)
            property("font-size", "12px")
            property("font-weight", "600")
            property("white-space", "nowrap")
        }

        ".$bannerTitleClass" {
            property("font-size", "17px")
            property("font-weight", "700")
            property("line-height", "1.3")
            property("max-width", "100%")
            property("margin", "7px 0 0")
            property("min-width", "0")
            property("overflow-wrap", "anywhere")
            property("width", "100%")
        }

        ".$bannerBodyClass" {
            property("color", FlotViewTokens.muted)
            property("font-size", "13px")
            property("max-width", "100%")
            property("margin", "8px 0 0")
            property("min-width", "0")
            property("overflow-wrap", "anywhere")
            property("width", "100%")
        }

        ".$bannerActionsClass" {
            property("display", "grid")
            property("gap", "8px")
            property("grid-template-columns", "repeat(2, minmax(0, 1fr))")
            property("margin-top", "12px")
            property("min-width", "0")
            property("width", "100%")
        }

        ".$bannerActionsClass > .$actionClass" {
            property("min-width", "0")
            property("width", "100%")
        }

        ".$modeClass" {
            property("background-color", FlotViewTokens.raised)
            property("border", "1px solid ${FlotViewTokens.line}")
            property("display", "grid")
            property("grid-template-columns", "repeat(2, minmax(96px, 1fr))")
            property("max-width", "100%")
            property("min-width", "0")
            property("width", "100%")
        }

        ".$modeOptionClass" {
            property("background-color", FlotViewTokens.raised)
            property("border", "0")
            property("color", FlotViewTokens.muted)
            property("cursor", "pointer")
            property("font-size", "13px")
            property("font-weight", "600")
            property("min-height", "44px")
            property("padding", "10px 14px")
            property("text-transform", "uppercase")
        }

        ".$modeOptionClass + .$modeOptionClass" {
            property("border-left", "1px solid ${FlotViewTokens.line}")
        }

        ".$modeActiveClass, .$modeActiveClass:hover:not(:disabled)" {
            property("background-color", FlotViewTokens.ink)
            property("color", FlotViewTokens.raised)
        }

        ".$setpointListClass" {
            property("border-bottom", "1px solid ${FlotViewTokens.line}")
            property("max-width", "100%")
            property("margin-top", "10px")
            property("min-width", "0")
            property("width", "100%")
        }

        ".$setpointRowClass" {
            property("align-items", "center")
            property("border-top", "1px solid ${FlotViewTokens.line}")
            property("display", "grid")
            property("gap", "12px")
            property("grid-template-columns", "minmax(0, 1fr) auto")
            property("max-width", "100%")
            property("min-height", "104px")
            property("min-width", "0")
            property("padding", "10px 0")
            property("width", "100%")
        }

        ".$setpointPendingClass" {
            property("background-color", FlotViewTokens.pendingBackground)
            property("box-shadow", "inset 3px 0 0 ${FlotViewTokens.warning}")
            property("padding-left", "10px")
            property("padding-right", "10px")
        }

        ".$setpointDisabledClass" {
            property("background-color", FlotViewTokens.surface)
            property("color", FlotViewTokens.muted)
        }

        ".$setpointTextClass" {
            property("max-width", "100%")
            property("min-width", "0")
            property("overflow-wrap", "anywhere")
            property("width", "100%")
        }

        ".$setpointLabelClass" {
            property("font-size", "15px")
            property("font-weight", "600")
        }

        ".$setpointDetailsClass" {
            property("color", FlotViewTokens.muted)
            property("font-size", "13px")
            property("margin-top", "2px")
        }

        ".$setpointPlanClass" {
            property("color", FlotViewTokens.muted)
            property("font-size", "12px")
            property("margin-top", "5px")
        }

        ".$setpointMessageClass" {
            property("border", "1px solid var(--flotview-tone)")
            property("color", "var(--flotview-tone)")
            property("font-size", "12px")
            property("font-weight", "600")
            property("grid-column", "1 / -1")
            property("padding", "8px 10px")
        }

        ".$stepperClass" {
            property("align-items", "stretch")
            property("display", "grid")
            property("gap", "6px")
            property("grid-template-columns", "72px 72px 72px")
        }

        ".$stepperButtonClass" {
            property("align-items", "center")
            property("background-color", FlotViewTokens.raised)
            property("border", "1px solid ${FlotViewTokens.line}")
            property("color", FlotViewTokens.ink)
            property("cursor", "pointer")
            property("display", "inline-flex")
            property("font-family", "\"IBM Plex Mono\", \"SFMono-Regular\", Consolas, monospace")
            property("font-size", "26px")
            property("font-weight", "700")
            property("height", "72px")
            property("justify-content", "center")
            property("width", "72px")
        }

        ".$stepperButtonClass:hover:not(:disabled), .$stepperButtonClass:focus-visible" {
            property("border-color", FlotViewTokens.info)
            property("color", FlotViewTokens.info)
        }

        ".$stepperButtonClass:disabled" {
            property("background-color", FlotViewTokens.track)
            property("border-color", FlotViewTokens.line)
            property("color", FlotViewTokens.muted)
        }

        ".$stepperValueClass" {
            property("align-items", "center")
            property("display", "flex")
            property("flex-direction", "column")
            property("height", "72px")
            property("justify-content", "center")
            property("min-width", "72px")
            property("text-align", "center")
        }

        ".$stepperNumberClass" {
            property("font-family", "\"IBM Plex Mono\", \"SFMono-Regular\", Consolas, monospace")
            property("font-size", "26px")
            property("font-variant-numeric", "tabular-nums")
            property("font-weight", "700")
            property("line-height", "1")
        }

        ".$stepperUnitClass" {
            property("color", FlotViewTokens.muted)
            property("font-size", "11px")
            property("margin-top", "3px")
        }

        ".$actionClass" {
            property("border", "1px solid ${FlotViewTokens.line}")
            property("cursor", "pointer")
            property("font-size", "14px")
            property("font-weight", "700")
            property("max-width", "100%")
            property("min-height", "56px")
            property("min-width", "120px")
            property("padding", "12px 18px")
        }

        ".$actionNeutralClass" {
            property("background-color", FlotViewTokens.raised)
            property("color", FlotViewTokens.ink)
        }

        ".$actionPrimaryClass" {
            property("background-color", FlotViewTokens.info)
            property("border-color", FlotViewTokens.info)
            property("color", FlotViewTokens.raised)
        }

        ".$actionSuccessClass" {
            property("background-color", FlotViewTokens.ok)
            property("border-color", FlotViewTokens.ok)
            property("color", FlotViewTokens.raised)
        }

        ".$actionDangerClass" {
            property("background-color", FlotViewTokens.alarm)
            property("border-color", FlotViewTokens.alarm)
            property("color", FlotViewTokens.raised)
        }

        ".$actionClass:hover:not(:disabled)" {
            property("filter", "brightness(0.94)")
        }

        ".$actionClass:disabled" {
            property("background-color", FlotViewTokens.track)
            property("border-color", FlotViewTokens.line)
            property("color", FlotViewTokens.muted)
        }

        ".$sendRowClass" {
            property("align-items", "center")
            property("background-color", FlotViewTokens.surface)
            property("border", "1px solid ${FlotViewTokens.line}")
            property("display", "grid")
            property("gap", "12px")
            property("grid-template-columns", "minmax(300px, 0.75fr) minmax(0, 1fr)")
            property("max-width", "100%")
            property("min-width", "0")
            property("padding", "8px")
            property("width", "100%")
        }

        ".$sendButtonClass" {
            property("height", "64px")
            property("width", "100%")
        }

        ".$sendMessageClass" {
            property("color", FlotViewTokens.muted)
            property("font-size", "13px")
        }

        ".$sendPendingClass" {
            property("color", FlotViewTokens.warning)
            property("font-weight", "600")
        }

        ".$sendErrorClass" {
            property("color", FlotViewTokens.alarm)
            property("font-weight", "600")
        }

        ".$stateClass" {
            property("align-items", "center")
            property("background-color", FlotViewTokens.raised)
            property("border", "1px solid ${FlotViewTokens.line}")
            property("display", "flex")
            property("flex-direction", "column")
            property("justify-content", "center")
            property("max-width", "100%")
            property("min-height", "180px")
            property("min-width", "0")
            property("padding", "24px")
            property("text-align", "center")
            property("width", "100%")
        }

        ".$stateTitleClass" {
            property("font-size", "21px")
            property("font-weight", "700")
            property("margin", "8px 0 0")
        }

        ".$stateDetailClass" {
            property("color", FlotViewTokens.muted)
            property("font-size", "13px")
            property("margin", "8px 0 0")
            property("max-width", "620px")
        }

        ".$stateDetailClass + .$actionClass" {
            property("margin-top", "16px")
        }

        ".$stateLoadingClass" {
            property("border-left", "6px solid ${FlotViewTokens.info}")
        }

        ".$stateErrorClass" {
            property("border-left", "6px solid ${FlotViewTokens.alarm}")
        }

        ".$stateEmptyClass" {
            property("color", FlotViewTokens.muted)
        }

        ".$stateDarkClass" {
            property("background-color", FlotViewTokens.process)
            property("border-color", FlotViewTokens.processGrid)
            property("color", FlotViewTokens.raised)
            property("min-height", "280px")
        }

        ".$stateDarkClass .$stateDetailClass" {
            property("color", FlotViewTokens.line)
        }

        ".$dashboardCellButtonClass" {
            property("background-color", FlotViewTokens.surface)
            property("border", "1px solid ${FlotViewTokens.line}")
            property("color", FlotViewTokens.ink)
            property("cursor", "pointer")
            property("font-size", "14px")
            property("font-weight", "600")
            property("max-width", "100%")
            property("min-height", "44px")
            property("min-width", "0")
            property("overflow", "hidden")
            property("padding", "9px 14px")
            property("text-overflow", "ellipsis")
            property("white-space", "nowrap")
        }

        ".$dashboardCellButtonClass:hover, .$dashboardCellButtonClass:focus-visible" {
            property("border-color", FlotViewTokens.info)
        }

        ".$dashboardHeaderStatusClass" {
            property("align-self", "stretch")
            property("display", "flex")
            property("flex", "1 1 160px")
            property("max-width", "100%")
            property("margin-right", "auto")
            property("min-width", "0")
        }

        ".$dashboardHeaderStatusClass > .$statusChipClass" {
            property("flex", "1 1 auto")
            property("width", "100%")
        }

        ".$dashboardOperatorClass" {
            property("align-items", "center")
            property("background-color", FlotViewTokens.raised)
            property("border", "1px solid ${FlotViewTokens.line}")
            property("display", "flex")
            property("flex", "1 1 180px")
            property("gap", "8px")
            property("max-width", "100%")
            property("min-height", "44px")
            property("min-width", "0")
            property("padding", "5px 10px")
        }

        ".$dashboardOperatorIdentityClass" {
            property("flex", "1 1 auto")
            property("line-height", "1.15")
            property("max-width", "100%")
            property("min-width", "0")
        }

        ".$dashboardOperatorNameClass" {
            property("font-size", "13px")
            property("font-weight", "700")
            property("overflow", "hidden")
            property("text-overflow", "ellipsis")
            property("white-space", "nowrap")
        }

        ".$dashboardOperatorDetailsClass" {
            property("color", FlotViewTokens.muted)
            property("font-family", "\"IBM Plex Mono\", \"SFMono-Regular\", Consolas, monospace")
            property("font-size", "10px")
            property("margin-top", "2px")
            property("overflow", "hidden")
            property("text-overflow", "ellipsis")
            property("white-space", "nowrap")
        }

        ".$dashboardTimeClass" {
            property("font-family", "\"IBM Plex Mono\", \"SFMono-Regular\", Consolas, monospace")
            property("font-size", "14px")
            property("font-variant-numeric", "tabular-nums")
            property("font-weight", "600")
            property("white-space", "nowrap")
        }

        ".$dashboardVideoContentClass" {
            property("display", "grid")
            property("height", "100%")
            property("inset", "0")
            property("min-height", "0")
            property("min-width", "0")
            property("place-items", "stretch")
            property("position", "absolute")
            property("width", "100%")
            property("z-index", "0")
        }

        ".$dashboardVideoContentClass > *" {
            property("height", "100%")
            property("max-height", "100%")
            property("max-width", "100%")
            property("min-height", "0")
            property("min-width", "0")
            property("object-fit", "cover")
            property("width", "100%")
        }

        ".$dashboardVideoPlaceholderClass" {
            property("background-color", FlotViewTokens.process)
            property(
                "background-image",
                "radial-gradient(circle, transparent 0 8px, rgba(201, 201, 197, .28) 9px 10px, transparent 11px), " +
                    "radial-gradient(circle, transparent 0 5px, rgba(201, 201, 197, .18) 6px 7px, transparent 8px)",
            )
            property("background-position", "0 0, 24px 24px")
            property("background-size", "48px 48px")
            property("height", "100%")
            property("min-height", "0")
            property("width", "100%")
        }

        ".$dashboardVideoBadgeClass" {
            property("align-items", "center")
            property("background", "rgba(26, 26, 25, .82)")
            property("color", FlotViewTokens.raised)
            property("display", "inline-flex")
            property("font-family", "\"IBM Plex Mono\", \"SFMono-Regular\", Consolas, monospace")
            property("font-size", "11px")
            property("font-weight", "600")
            property("gap", "6px")
            property("max-width", "calc(100% - 20px)")
            property("min-width", "0")
            property("overflow", "hidden")
            property("padding", "5px 8px")
            property("position", "absolute")
            property("text-overflow", "ellipsis")
            property("white-space", "nowrap")
            property("z-index", "1")
        }

        ".$dashboardVideoBadgeTopLeftClass" {
            property("left", "10px")
            property("top", "10px")
        }

        ".$dashboardVideoBadgeTopRightClass" {
            property("right", "10px")
            property("top", "10px")
        }

        ".$dashboardVideoBadgeBottomLeftClass" {
            property("bottom", "10px")
            property("left", "10px")
        }

        ".$dashboardVideoBadgeBottomRightClass" {
            property("bottom", "10px")
            property("right", "10px")
        }

        ".$dashboardAiUnavailableClass" {
            property("background-color", FlotViewTokens.raised)
            property("border", "1px solid ${FlotViewTokens.line}")
            property("border-left", "6px solid var(--flotview-tone)")
            property("max-width", "100%")
            property("min-width", "0")
            property("padding", "12px 14px")
            property("width", "100%")
        }

        ".$dashboardAiUnavailableTitleClass" {
            property("font-size", "15px")
            property("font-weight", "700")
            property("margin-top", "5px")
        }

        ".$dashboardAiUnavailableDetailClass" {
            property("color", FlotViewTokens.muted)
            property("font-size", "13px")
            property("margin-top", "5px")
        }

        ".$dashboardControlsCardClass" {
            property("align-self", "stretch")
            property("display", "flex")
            property("flex", "1 1 auto")
            property("flex-direction", "column")
            property("height", "100%")
            property("max-height", "100%")
            property("max-width", "100%")
            property("min-height", "0")
            property("min-width", "0")
            property("overflow", "hidden")
            property("width", "100%")
        }

        ".$dashboardControlsHeaderClass" {
            property("align-items", "center")
            property("display", "flex")
            property("flex", "0 0 auto")
            property("gap", "12px")
            property("justify-content", "space-between")
            property("min-width", "0")
            property("padding-bottom", "10px")
            property("width", "100%")
        }

        ".$dashboardControlsTitleClass" {
            property("color", FlotViewTokens.muted)
            property("font-size", "12px")
            property("font-weight", "600")
            property("letter-spacing", "0.05em")
            property("min-width", "0")
            property("overflow-wrap", "anywhere")
            property("text-transform", "uppercase")
        }

        ".$dashboardControlsCardClass > .$setpointListClass" {
            property("flex", "1 1 auto")
            property("min-height", "0")
            property("overflow", "auto")
        }

        ".$dashboardPolicyClass" {
            property("border-top", "1px solid ${FlotViewTokens.line}")
            property("color", FlotViewTokens.muted)
            property("flex", "0 0 auto")
            property("font-size", "12px")
            property("margin-top", "10px")
            property("min-width", "0")
            property("overflow-wrap", "anywhere")
            property("padding-top", "10px")
            property("width", "100%")
        }

        ".$dashboardSendDockClass" {
            property("flex", "0 0 auto")
            property("margin-top", "auto")
            property("min-width", "0")
            property("padding-top", "12px")
            property("width", "100%")
        }

        ".$dashboardDistributionClass" {
            property("background-color", FlotViewTokens.raised)
            property("border", "1px solid ${FlotViewTokens.line}")
            property("max-width", "100%")
            property("min-width", "0")
            property("padding", "10px")
            property("width", "100%")
        }

        ".$dashboardDistributionHeaderClass" {
            property("align-items", "baseline")
            property("display", "flex")
            property("gap", "12px")
            property("justify-content", "space-between")
        }

        ".$dashboardDistributionSummaryClass" {
            property("font-family", "\"IBM Plex Mono\", \"SFMono-Regular\", Consolas, monospace")
            property("font-size", "12px")
            property("font-weight", "600")
            property("white-space", "nowrap")
        }

        ".$dashboardDistributionTrackClass" {
            property("background-color", FlotViewTokens.track)
            property("display", "flex")
            property("height", "22px")
            property("margin-top", "8px")
            property("overflow", "hidden")
            property("width", "100%")
        }

        ".$dashboardDistributionSegmentClass" {
            property("border-right", "2px solid ${FlotViewTokens.raised}")
            property("min-width", "2px")
        }

        ".$dashboardDistributionSegmentClass:nth-child(3n + 1)" {
            property("background-color", "#B8B8B2")
        }

        ".$dashboardDistributionSegmentClass:nth-child(3n + 2)" {
            property("background-color", "#8E8E87")
        }

        ".$dashboardDistributionSegmentClass:nth-child(3n)" {
            property("background-color", "#A8A8A1")
            property("border-right", "0")
        }

        ".$dashboardDistributionLegendClass" {
            property("display", "grid")
            property("gap", "8px")
            property("grid-template-columns", "repeat(3, minmax(0, 1fr))")
            property("margin-top", "7px")
        }

        ".$dashboardDistributionLegendItemClass" {
            property("align-items", "center")
            property("color", FlotViewTokens.muted)
            property("display", "flex")
            property("font-size", "11px")
            property("gap", "5px")
            property("min-width", "0")
        }

        ".$dashboardDistributionSwatchClass" {
            property("background-color", FlotViewTokens.muted)
            property("display", "inline-block")
            property("flex", "0 0 7px")
            property("height", "7px")
            property("width", "7px")
        }

        ".$dashboardDistributionLegendItemClass:nth-child(3n + 1) .$dashboardDistributionSwatchClass" {
            property("background-color", "#B8B8B2")
        }

        ".$dashboardDistributionLegendItemClass:nth-child(3n + 2) .$dashboardDistributionSwatchClass" {
            property("background-color", "#8E8E87")
        }

        ".$dashboardDistributionLegendItemClass:nth-child(3n) .$dashboardDistributionSwatchClass" {
            property("background-color", "#A8A8A1")
        }

        ".$dashboardDistributionLegendItemClass:nth-child(2)" {
            property("justify-content", "center")
        }

        ".$dashboardDistributionLegendItemClass:nth-child(3)" {
            property("justify-content", "flex-end")
        }

        ".$dashboardLockedClass" {
            property("display", "flex")
            property("grid-column", "1 / -1")
            property("height", "100%")
            property("min-height", "0")
            property("min-width", "0")
            property("width", "100%")
        }

        ".$dashboardLockedClass > .$stateClass" {
            property("flex", "1")
            property("width", "100%")
        }

        ".$toneNeutralClass" {
            property("--flotview-tone", FlotViewTokens.muted)
        }

        ".$toneInfoClass" {
            property("--flotview-tone", FlotViewTokens.info)
        }

        ".$toneSuccessClass" {
            property("--flotview-tone", FlotViewTokens.ok)
        }

        ".$toneWarningClass" {
            property("--flotview-tone", FlotViewTokens.warning)
        }

        ".$toneErrorClass" {
            property("--flotview-tone", FlotViewTokens.alarm)
        }

        media("(max-width: 960px)") {
            ".$bodyClass" {
                property("grid-template-columns", "minmax(0, 1fr)")
                property("grid-template-rows", "auto")
            }

            ".$primaryColumnClass, .$controlsColumnClass" {
                property("grid-column", "1")
            }

        }

        media("(max-width: 600px)") {
            ".$panelClass" {
                property("padding", "8px")
            }

            ".$headerClass" {
                property("align-items", "stretch")
                property("flex-direction", "column")
            }

            ".$headerActionsClass" {
                property("justify-content", "flex-start")
                property("width", "100%")
            }

            ".$dashboardHeaderStatusClass" {
                property("margin-right", "0")
                property("width", "100%")
            }

            ".$dashboardOperatorClass" {
                property("max-width", "100%")
            }

            ".$metricGridClass" {
                property("grid-template-columns", "repeat(2, minmax(0, 1fr))")
            }

            ".$bannerActionsClass" {
                property("grid-template-columns", "minmax(0, 1fr)")
            }

            ".$setpointRowClass" {
                property("grid-template-columns", "minmax(0, 1fr)")
            }

            ".$stepperClass" {
                property("justify-self", "center")
            }

            ".$sendRowClass" {
                property("grid-template-columns", "minmax(0, 1fr)")
            }

            ".$tabsLabelClass" {
                property("display", "none")
            }

            ".$dashboardControlsHeaderClass" {
                property("align-items", "stretch")
                property("flex-direction", "column")
            }

            ".$dashboardDistributionLegendClass" {
                property("grid-template-columns", "minmax(0, 1fr)")
            }

            ".$dashboardDistributionLegendItemClass:nth-child(n)" {
                property("justify-content", "flex-start")
            }
        }
        StyleSheetsAggregator.addStyleSheet(this)
    }
}

/** Emits [FlotViewStyleSheet] into the current Compose Web document. */
@Composable
fun InstallFlotViewStyles() {
    Style(FlotViewStyleSheet)
}

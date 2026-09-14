package space.kscience.frameswork.features.common.web.ui

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text

/**
 * Display state for the operator-panel header.
 *
 * @property cellLabel currently selected process cell.
 * @property processStatus optional short process-condition message.
 * @property processStatusTone semantic colour for [processStatus].
 * @property operatorName display name of the current operator.
 * @property operatorDetails secondary operator identity or authorization details.
 * @property operatorTone semantic colour for the operator status.
 * @property currentTime display-ready local time.
 */
data class FlotViewDashboardHeader(
    val cellLabel: String,
    val processStatus: String? = null,
    val processStatusTone: FlotViewTone = FlotViewTone.Warning,
    val operatorName: String,
    val operatorDetails: String,
    val operatorTone: FlotViewTone = FlotViewTone.Success,
    val currentTime: String,
)

/** State of the process-video area in a FlotView dashboard. */
sealed interface FlotViewDashboardVideo {
    /**
     * Video is available for rendering by the dashboard's video-content slot.
     *
     * @property liveLabel primary live-stream badge.
     * @property liveTone semantic colour for [liveLabel].
     * @property aiLabel optional AI-processing badge.
     * @property aiTone semantic colour for [aiLabel].
     * @property bottomLeftLabel optional lower-left video annotation.
     * @property bottomRightLabel optional lower-right video annotation.
     */
    data class Online(
        val liveLabel: String,
        val liveTone: FlotViewTone = FlotViewTone.Error,
        val aiLabel: String? = null,
        val aiTone: FlotViewTone = FlotViewTone.Success,
        val bottomLeftLabel: String? = null,
        val bottomRightLabel: String? = null,
    ) : FlotViewDashboardVideo

    /**
     * Video is unavailable and should be replaced by an explanatory state panel.
     *
     * @property eyebrow short category such as a connection status.
     * @property title primary user-facing status.
     * @property detail additional recovery or diagnostic information.
     */
    data class Offline(
        val eyebrow: String,
        val title: String,
        val detail: String,
    ) : FlotViewDashboardVideo
}

/** Availability state for the decision-support recommendation area. */
sealed interface FlotViewDashboardRecommendation {
    /**
     * A recommendation that the operator may accept or dismiss.
     *
     * @property title recommended action.
     * @property description evidence or reasoning shown below [title].
     * @property eyebrow short recommendation category.
     * @property confidence optional display-ready confidence value.
     * @property tone semantic colour for the recommendation.
     * @property actionsDisabled whether accept and dismiss actions are unavailable.
     */
    data class Available(
        val title: String,
        val description: String,
        val eyebrow: String = "Recommendation",
        val confidence: String? = null,
        val tone: FlotViewTone = FlotViewTone.Info,
        val actionsDisabled: Boolean = false,
    ) : FlotViewDashboardRecommendation

    /**
     * Explanation shown when no actionable recommendation is available.
     *
     * @property eyebrow short availability category.
     * @property title primary user-facing status.
     * @property detail additional context.
     * @property tone semantic colour for the status.
     */
    data class Unavailable(
        val eyebrow: String,
        val title: String,
        val detail: String,
        val tone: FlotViewTone = FlotViewTone.Neutral,
    ) : FlotViewDashboardRecommendation
}

/**
 * One labelled percentage in a bubble-size distribution.
 *
 * @property label human-readable range or category.
 * @property percent integer share from 0 through 100.
 * @throws IllegalArgumentException if [percent] is outside `0..100`.
 */
data class FlotViewDistributionSegment(
    val label: String,
    val percent: Int,
) {
    init {
        require(percent in 0..100) { "Distribution percent must be between 0 and 100" }
    }
}

/**
 * Display-ready bubble-size distribution rendered beneath the process video.
 *
 * @property title section heading.
 * @property summary compact aggregate such as the mean size.
 * @property segments ordered distribution portions; callers decide whether their total is 100.
 */
data class FlotViewBubbleDistribution(
    val title: String,
    val summary: String,
    val segments: List<FlotViewDistributionSegment>,
)

/**
 * State of the dashboard's process-control section.
 *
 * @property mode current automatic/manual control mode.
 * @property setpoints ordered editable or read-only setpoints.
 * @property sendState current state of the send operation.
 * @property modeEnabled whether the operator may change [mode].
 * @property policyMessage optional mode policy shown beside the controls.
 */
data class FlotViewDashboardControls(
    val mode: FlotViewMode,
    val setpoints: List<FlotViewSetpoint>,
    val sendState: FlotViewSendState,
    val modeEnabled: Boolean = true,
    val policyMessage: String? = null,
)

/** Whether the dashboard session may expose live operator controls. */
sealed interface FlotViewDashboardSession {
    /** The dashboard is active and its normal content may be rendered. */
    data object Active : FlotViewDashboardSession

    /**
     * The dashboard is locked and its body is replaced by a warning.
     *
     * @property eyebrow short lock category.
     * @property title primary lock message.
     * @property detail explanation or recovery guidance.
     */
    data class Locked(
        val eyebrow: String,
        val title: String,
        val detail: String,
    ) : FlotViewDashboardSession
}

/**
 * Stateless event callbacks emitted by [FlotViewDashboard].
 *
 * Every callback defaults to a no-op so callers can opt into only relevant interactions. Indexed
 * callbacks use the corresponding index in the model list.
 *
 * @property onCellSelectorClick opens or cycles the cell selector.
 * @property onLayerSelected handles selection in `FlotViewDashboardModel.aiLayers`.
 * @property onRecommendationAccepted accepts the current recommendation.
 * @property onRecommendationDismissed dismisses the current recommendation.
 * @property onModeSelected handles a new automatic/manual mode.
 * @property onSetpointDecrement decrements a setpoint by list index.
 * @property onSetpointIncrement increments a setpoint by list index.
 * @property onSend sends the pending setpoint changes.
 */
data class FlotViewDashboardActions(
    val onCellSelectorClick: () -> Unit = {},
    val onLayerSelected: (index: Int) -> Unit = {},
    val onRecommendationAccepted: () -> Unit = {},
    val onRecommendationDismissed: () -> Unit = {},
    val onModeSelected: (FlotViewMode) -> Unit = {},
    val onSetpointDecrement: (index: Int) -> Unit = {},
    val onSetpointIncrement: (index: Int) -> Unit = {},
    val onSend: () -> Unit = {},
)

/**
 * User-facing labels used by the dashboard renderer.
 *
 * The defaults are Russian reference-design copy. Supply another instance to localize the view.
 * The properties cover selector accessibility, section labels, recommendation actions, send-state
 * messages, setpoint annotations, adjustment accessibility, video fallback, and AUTO-mode policy.
 */
data class FlotViewDashboardStrings(
    val cellSelectorLabel: String = "Выбрать ячейку",
    val aiLayersLabel: String = "AI-СЛОИ",
    val controlsTitle: String = "УПРАВЛЕНИЕ · УСТАВКИ",
    val modeLabel: String = "Режим управления",
    val applyRecommendation: String = "Применить рекомендацию",
    val dismissRecommendation: String = "Отклонить",
    val sendSetpoints: String = "Отправить уставки в АРС",
    val noUnsavedChanges: String = "нет несохранённых изменений",
    val sending: String = "Отправка…",
    val waitingForConfirmation: String = "ожидание подтверждения АРС",
    val pendingChanges: (Int) -> String = { changes -> "$changes изм. ожидают отправки" },
    val actualLabel: String = "факт",
    val stepLabel: String = "шаг",
    val limitsLabel: String = "границы",
    val decreaseLabel: String = "Уменьшить уставку",
    val increaseLabel: String = "Увеличить уставку",
    val videoPlaceholderLabel: String = "Заглушка технологического видео",
    val autoPolicy: String =
        "Контур в AUTO — уставки задаёт АРС. Для ручной коррекции переключите MANUAL.",
)

/**
 * Complete immutable render model for [FlotViewDashboard].
 *
 * @property title panel heading.
 * @property subtitle process-line context shown below [title].
 * @property header operator, cell, status, and time information.
 * @property video online/offline state for the visual surface.
 * @property aiLayers selectable AI overlays.
 * @property videoMetrics metrics derived from the process video.
 * @property processMetrics general process measurements.
 * @property recommendation available or unavailable decision-support state.
 * @property controls process-control state and setpoints.
 * @property bubbleDistribution optional bubble-size visualization.
 * @property session active or locked session state.
 */
data class FlotViewDashboardModel(
    val title: String,
    val subtitle: String,
    val header: FlotViewDashboardHeader,
    val video: FlotViewDashboardVideo,
    val aiLayers: List<FlotViewTab>,
    val videoMetrics: List<FlotViewMetric>,
    val processMetrics: List<FlotViewMetric>,
    val recommendation: FlotViewDashboardRecommendation,
    val controls: FlotViewDashboardControls,
    val bubbleDistribution: FlotViewBubbleDistribution? = null,
    val session: FlotViewDashboardSession = FlotViewDashboardSession.Active,
) {
    companion object {
        /** Static sample matching page 1 of the supplied PDF. Never use it as live process data. */
        fun referencePreview(): FlotViewDashboardModel = FlotViewDashboardModel(
            title = "Флотатор · цеховая панель",
            subtitle = "Медный контур · Линия 1 · управляющий режим",
            header = FlotViewDashboardHeader(
                cellLabel = "Ячейка R2",
                processStatus = "Пена: нагруженная · тренд к перегрузу",
                processStatusTone = FlotViewTone.Warning,
                operatorName = "Смирнов А. В.",
                operatorDetails = "таб. 04127 · флотатор · упр. разрешено",
                currentTime = "10:33",
            ),
            video = FlotViewDashboardVideo.Online(
                liveLabel = "LIVE · R2",
                aiLabel = "AI · доверие 89%",
                bottomLeftLabel = "Губа: не видна ✓",
                bottomRightLabel = "1280×720 · 25 к/с",
            ),
            aiLayers = listOf(
                FlotViewTab("ROI", selected = true),
                FlotViewTab("Векторы"),
                FlotViewTab("Контуры пузырей"),
            ),
            videoMetrics = listOf(
                FlotViewMetric("Скорость", "2.1", "см/с", FlotViewTone.Warning),
                FlotViewMetric("Пузырь", "11", "мм"),
                FlotViewMetric("Стабильн.", "0.84"),
                FlotViewMetric("Цвет", "0.44"),
                FlotViewMetric("Высота", "46", "мм"),
                FlotViewMetric("Загрузка", "0.74"),
            ),
            processMetrics = listOf(
                FlotViewMetric("Уровень", "63", "%", FlotViewTone.Warning, "план 56"),
                FlotViewMetric("Воздух", "31", "м³/ч", FlotViewTone.Warning, "план 34"),
                FlotViewMetric("pH", "10.7", hint = "план 10.8"),
                FlotViewMetric("Питание Cu", "0.62", "%", hint = "mass-pull 0.70"),
            ),
            recommendation = FlotViewDashboardRecommendation.Available(
                eyebrow = "СППР · РЕКОМЕНДАЦИЯ",
                confidence = "доверие 86%",
                title = "Снизить уставку уровня 62 → 58 % и поднять воздух 31 → 34 м³/ч",
                description =
                    "Пена уплотняется, скорость растёт (2.1 см/с), уровень выше плана — " +
                        "тренд к перегрузу. Возврат к плановому режиму стабилизирует извлечение.",
            ),
            controls = FlotViewDashboardControls(
                mode = FlotViewMode.Manual,
                setpoints = listOf(
                    FlotViewSetpoint(
                        label = "Уровень пульпы",
                        actual = "63",
                        target = "62",
                        unit = "%",
                        plan = "план 56",
                        step = "1",
                        limits = "45–70",
                    ),
                    FlotViewSetpoint(
                        label = "Расход воздуха",
                        actual = "31",
                        target = "31",
                        unit = "м³/ч",
                        plan = "план 34",
                        step = "1",
                        limits = "20–45",
                    ),
                ),
                sendState = FlotViewSendState.Idle,
            ),
            bubbleDistribution = FlotViewBubbleDistribution(
                title = "РАСПРЕДЕЛЕНИЕ ПУЗЫРЕЙ",
                summary = "средний 11 мм",
                segments = listOf(
                    FlotViewDistributionSegment("мелкие <8", 22),
                    FlotViewDistributionSegment("средние 8–16", 61),
                    FlotViewDistributionSegment("крупные >16", 17),
                ),
            ),
        )
    }
}

/** Renders the static PDF reference screen without requiring a model or callbacks. */
@Composable
fun FlotViewDashboard() {
    FlotViewDashboard(model = FlotViewDashboardModel.referencePreview())
}

/**
 * Complete FlotView operator panel from the supplied design.
 *
 * The component is deliberately stateless: the caller owns [model] and handles all events through
 * [actions]. [videoContent] can render an image, stream, canvas, or AI overlay; the default draws a
 * neutral process placeholder suitable for previews.
 */
@Composable
fun FlotViewDashboard(
    model: FlotViewDashboardModel,
    actions: FlotViewDashboardActions = FlotViewDashboardActions(),
    strings: FlotViewDashboardStrings = FlotViewDashboardStrings(),
    videoContent: @Composable () -> Unit = {
        FlotViewDashboardVideoPlaceholder(strings.videoPlaceholderLabel)
    },
) {
    FlotViewPanel(
        title = model.title,
        subtitle = model.subtitle,
        headerContent = {
            val header = model.header
            FlotViewDashboardHeaderContent(
                cellSelectorLabel = strings.cellSelectorLabel,
                operatorLabel = "${header.operatorName}: ${header.operatorDetails}",
                currentTimeLabel = header.currentTime,
                cellSelector = { Text("${header.cellLabel} ▾") },
                processStatus = header.processStatus?.let { status -> { Text(status) } },
                processStatusTone = header.processStatusTone,
                operatorName = { Text(header.operatorName) },
                operatorDetails = { Text(header.operatorDetails) },
                operatorTone = header.operatorTone,
                currentTime = { Text(header.currentTime) },
                onCellSelectorClick = actions.onCellSelectorClick,
            )
        },
    ) {
        when (val session = model.session) {
            FlotViewDashboardSession.Active -> FlotViewDashboardBody(
                model = model,
                actions = actions,
                strings = strings,
                videoContent = videoContent,
            )

            is FlotViewDashboardSession.Locked -> {
                Div(attrs = { classes(FlotViewStyleSheet.dashboardLockedClass) }) {
                    FlotViewStatePanel(
                        eyebrow = session.eyebrow,
                        title = session.title,
                        detail = session.detail,
                        tone = FlotViewTone.Warning,
                    )
                }
            }
        }
    }
}

/**
 * Renders the cell selector, optional process status, operator identity, and current time.
 *
 * Text and markup are supplied as composable slots, while the string labels provide accessible
 * names for the interactive and time elements.
 */
@Composable
fun FlotViewDashboardHeaderContent(
    cellSelectorLabel: String,
    operatorLabel: String,
    currentTimeLabel: String,
    cellSelector: @Composable () -> Unit,
    operatorName: @Composable () -> Unit,
    operatorDetails: @Composable () -> Unit,
    currentTime: @Composable () -> Unit,
    onCellSelectorClick: () -> Unit,
    processStatus: (@Composable () -> Unit)? = null,
    processStatusTone: FlotViewTone = FlotViewTone.Warning,
    operatorTone: FlotViewTone = FlotViewTone.Success,
) {
    Button(attrs = {
        classes(FlotViewStyleSheet.dashboardCellButtonClass)
        type(ButtonType.Button)
        attr("aria-label", cellSelectorLabel)
        onClick { onCellSelectorClick() }
    }) {
        cellSelector()
    }

    processStatus?.let {
        Div(attrs = { classes(FlotViewStyleSheet.dashboardHeaderStatusClass) }) {
            FlotViewStatusChip(
                tone = processStatusTone,
                content = it,
            )
        }
    }

    Div(attrs = {
        classes(FlotViewStyleSheet.dashboardOperatorClass, operatorTone.dashboardToneClass)
        attr("role", "status")
        attr("aria-label", operatorLabel)
    }) {
        Span(attrs = {
            classes(FlotViewStyleSheet.statusDotClass)
            attr("aria-hidden", "true")
        })
        Div(attrs = { classes(FlotViewStyleSheet.dashboardOperatorIdentityClass) }) {
            Div(attrs = { classes(FlotViewStyleSheet.dashboardOperatorNameClass) }) {
                operatorName()
            }
            Div(attrs = { classes(FlotViewStyleSheet.dashboardOperatorDetailsClass) }) {
                operatorDetails()
            }
        }
    }

    Div(attrs = {
        classes(FlotViewStyleSheet.dashboardTimeClass)
        attr("aria-label", currentTimeLabel)
    }) {
        currentTime()
    }
}

/** Renders the active dashboard's video, metrics, recommendation, and process controls. */
@Composable
private fun FlotViewDashboardBody(
    model: FlotViewDashboardModel,
    actions: FlotViewDashboardActions,
    strings: FlotViewDashboardStrings,
    videoContent: @Composable () -> Unit,
) {
    FlotViewColumn {
        when (val video = model.video) {
            is FlotViewDashboardVideo.Online -> FlotViewDashboardVideoSurface(
                liveLabel = { Text(video.liveLabel) },
                liveTone = video.liveTone,
                aiLabel = video.aiLabel?.let { label -> { Text(label) } },
                aiTone = video.aiTone,
                bottomLeftLabel = video.bottomLeftLabel?.let { label -> { Text(label) } },
                bottomRightLabel = video.bottomRightLabel?.let { label -> { Text(label) } },
                videoContent = videoContent,
            )

            is FlotViewDashboardVideo.Offline -> FlotViewDashboardVideoUnavailable(video)
        }
        FlotViewTabs(
            tabs = model.aiLayers,
            onSelected = actions.onLayerSelected,
            label = strings.aiLayersLabel,
        )
        if (model.videoMetrics.isNotEmpty()) {
            FlotViewMetricGrid(model.videoMetrics)
        }
        model.bubbleDistribution?.let { FlotViewDashboardDistribution(it) }
    }

    FlotViewColumn(controls = true) {
        if (model.processMetrics.isNotEmpty()) {
            FlotViewMetricGrid(model.processMetrics)
        }
        when (val recommendation = model.recommendation) {
            is FlotViewDashboardRecommendation.Available -> FlotViewDashboardRecommendation(
                title = { Text(recommendation.title) },
                description = { Text(recommendation.description) },
                acceptLabel = strings.applyRecommendation,
                dismissLabel = strings.dismissRecommendation,
                eyebrow = recommendation.eyebrow,
                confidence = recommendation.confidence,
                tone = recommendation.tone,
                actionsDisabled = recommendation.actionsDisabled,
                onDismiss = actions.onRecommendationDismissed,
                onAccept = actions.onRecommendationAccepted,
            )

            is FlotViewDashboardRecommendation.Unavailable ->
                FlotViewDashboardRecommendationUnavailable(recommendation)
        }
        val controls = model.controls
        val policyMessage = controls.policyMessage
            ?: strings.autoPolicy.takeIf { controls.mode == FlotViewMode.Auto }
        FlotViewDashboardControls(
            title = { Text(strings.controlsTitle) },
            modeControl = {
                FlotViewModeSwitch(
                    selected = controls.mode,
                    onSelected = actions.onModeSelected,
                    enabled = controls.modeEnabled,
                    label = strings.modeLabel,
                )
            },
            setpoints = {
                controls.setpoints.forEachIndexed { index, setpoint ->
                    val disabled = setpoint.disabled || controls.mode == FlotViewMode.Auto
                    val details = buildList {
                        add(
                            "${strings.actualLabel} " +
                                formatFlotViewMeasurement(setpoint.actual, setpoint.unit)
                        )
                        setpoint.step?.let { add("${strings.stepLabel} $it") }
                        setpoint.limits?.let { add("${strings.limitsLabel} $it") }
                    }.joinToString(" · ")
                    FlotViewSetpointRow(
                        pending = setpoint.pending,
                        disabled = disabled,
                        messageTone = setpoint.messageTone,
                        message = setpoint.message?.let { message -> { Text(message) } },
                        stepper = {
                            FlotViewStepper(
                                value = setpoint.target,
                                unit = setpoint.unit,
                                label = setpoint.label,
                                onDecrement = { actions.onSetpointDecrement(index) },
                                onIncrement = { actions.onSetpointIncrement(index) },
                                decrementDisabled = disabled || setpoint.decrementDisabled,
                                incrementDisabled = disabled || setpoint.incrementDisabled,
                                decrementLabel = strings.decreaseLabel,
                                incrementLabel = strings.increaseLabel,
                            )
                        },
                    ) {
                        Div(attrs = { classes(FlotViewStyleSheet.setpointLabelClass) }) {
                            Text(setpoint.label)
                        }
                        Div(attrs = { classes(FlotViewStyleSheet.setpointDetailsClass) }) {
                            Text(details)
                        }
                        setpoint.plan?.let { plan ->
                            Div(attrs = { classes(FlotViewStyleSheet.setpointPlanClass) }) {
                                Text(plan)
                            }
                        }
                    }
                }
            },
            policy = policyMessage?.let { policy -> { Text(policy) } },
            sendControls = {
                FlotViewSendControls(
                    state = controls.sendState,
                    onSend = actions.onSend,
                    sendLabel = strings.sendSetpoints,
                    idleMessage = strings.noUnsavedChanges,
                    sendingLabel = strings.sending,
                    sendingMessage = strings.waitingForConfirmation,
                    pendingMessage = strings.pendingChanges,
                )
            },
        )
    }
}

/**
 * Renders a video/content surface with live, AI, and optional lower-corner status badges.
 *
 * [videoContent] is rendered below the badges and may contain a stream, image, canvas, or overlay.
 */
@Composable
fun FlotViewDashboardVideoSurface(
    liveLabel: @Composable () -> Unit,
    liveTone: FlotViewTone = FlotViewTone.Error,
    aiLabel: (@Composable () -> Unit)? = null,
    aiTone: FlotViewTone = FlotViewTone.Success,
    bottomLeftLabel: (@Composable () -> Unit)? = null,
    bottomRightLabel: (@Composable () -> Unit)? = null,
    videoContent: @Composable () -> Unit,
) {
    FlotViewVisualSurface {
        Div(attrs = { classes(FlotViewStyleSheet.dashboardVideoContentClass) }) {
            videoContent()
        }
        FlotViewDashboardVideoBadge(
            positionClass = FlotViewStyleSheet.dashboardVideoBadgeTopLeftClass,
            tone = liveTone,
            content = liveLabel,
        )
        aiLabel?.let {
            FlotViewDashboardVideoBadge(
                positionClass = FlotViewStyleSheet.dashboardVideoBadgeTopRightClass,
                tone = aiTone,
                content = it,
            )
        }
        bottomLeftLabel?.let {
            FlotViewDashboardVideoBadge(
                positionClass = FlotViewStyleSheet.dashboardVideoBadgeBottomLeftClass,
                content = it,
            )
        }
        bottomRightLabel?.let {
            FlotViewDashboardVideoBadge(
                positionClass = FlotViewStyleSheet.dashboardVideoBadgeBottomRightClass,
                content = it,
            )
        }
    }
}

/** Renders one positioned status badge over the process-video surface. */
@Composable
private fun FlotViewDashboardVideoBadge(
    positionClass: String,
    tone: FlotViewTone? = null,
    content: @Composable () -> Unit,
) {
    Div(attrs = {
        classes(FlotViewStyleSheet.dashboardVideoBadgeClass, positionClass)
        tone?.let { classes(it.dashboardToneClass) }
        attr("role", "status")
    }) {
        if (tone != null) {
            Span(attrs = {
                classes(FlotViewStyleSheet.statusDotClass)
                attr("aria-hidden", "true")
            })
        }
        content()
    }
}

/** Renders an offline-video explanation in place of process imagery. */
@Composable
private fun FlotViewDashboardVideoUnavailable(state: FlotViewDashboardVideo.Offline) {
    FlotViewVisualSurface {
        FlotViewVisualOverlay(
            eyebrow = state.eyebrow,
            title = state.title,
            detail = state.detail,
            tone = FlotViewTone.Error,
        )
    }
}

/** Renders the neutral placeholder used when no custom video content is supplied. */
@Composable
private fun FlotViewDashboardVideoPlaceholder(
    label: String = "Process video placeholder",
) {
    Div(attrs = {
        classes(FlotViewStyleSheet.dashboardVideoPlaceholderClass)
        attr("role", "img")
        attr("aria-label", label)
    })
}

/**
 * Dashboard-specific wrapper for an actionable [FlotViewRecommendationCard].
 *
 * The caller supplies composable title and description content plus localized action labels.
 */
@Composable
fun FlotViewDashboardRecommendation(
    title: @Composable () -> Unit,
    description: @Composable () -> Unit,
    acceptLabel: String,
    dismissLabel: String,
    eyebrow: String = "Recommendation",
    confidence: String? = null,
    tone: FlotViewTone = FlotViewTone.Info,
    actionsDisabled: Boolean = false,
    onDismiss: () -> Unit,
    onAccept: () -> Unit,
) {
    FlotViewRecommendationCard(
        title = title,
        description = description,
        eyebrow = eyebrow,
        confidence = confidence,
        tone = tone,
        onAccept = onAccept,
        onDismiss = onDismiss,
        acceptLabel = acceptLabel,
        dismissLabel = dismissLabel,
        actionsDisabled = actionsDisabled,
    )
}

/** Renders the non-actionable state of the recommendation area. */
@Composable
private fun FlotViewDashboardRecommendationUnavailable(
    state: FlotViewDashboardRecommendation.Unavailable,
) {
    Div(attrs = {
        classes(
            FlotViewStyleSheet.dashboardAiUnavailableClass,
            state.tone.dashboardToneClass,
        )
        attr("role", if (state.tone == FlotViewTone.Error) "alert" else "status")
    }) {
        Div(attrs = { classes(FlotViewStyleSheet.sectionTitleClass) }) {
            Text(state.eyebrow)
        }
        Div(attrs = { classes(FlotViewStyleSheet.dashboardAiUnavailableTitleClass) }) {
            Text(state.title)
        }
        Div(attrs = { classes(FlotViewStyleSheet.dashboardAiUnavailableDetailClass) }) {
            Text(state.detail)
        }
    }
}

/**
 * Lays out the dashboard control title, mode control, setpoints, optional policy, and send dock.
 *
 * All functional content is supplied as composable slots so callers retain state ownership.
 */
@Composable
fun FlotViewDashboardControls(
    title: @Composable () -> Unit,
    modeControl: @Composable () -> Unit,
    setpoints: @Composable () -> Unit,
    sendControls: @Composable () -> Unit,
    policy: (@Composable () -> Unit)? = null,
) {
    Div(attrs = {
        classes(
            FlotViewStyleSheet.cardClass,
            FlotViewStyleSheet.dashboardControlsCardClass,
        )
    }) {
        Div(attrs = { classes(FlotViewStyleSheet.dashboardControlsHeaderClass) }) {
            Div(attrs = { classes(FlotViewStyleSheet.dashboardControlsTitleClass) }) {
                title()
            }
            modeControl()
        }

        FlotViewSetpointList {
            setpoints()
        }

        policy?.let {
            Div(attrs = { classes(FlotViewStyleSheet.dashboardPolicyClass) }) {
                it()
            }
        }

        Div(attrs = { classes(FlotViewStyleSheet.dashboardSendDockClass) }) {
            sendControls()
        }
    }
}

/** Renders a proportional bubble distribution and its textual legend. */
@Composable
private fun FlotViewDashboardDistribution(distribution: FlotViewBubbleDistribution) {
    Div(attrs = { classes(FlotViewStyleSheet.dashboardDistributionClass) }) {
        Div(attrs = { classes(FlotViewStyleSheet.dashboardDistributionHeaderClass) }) {
            Div(attrs = { classes(FlotViewStyleSheet.sectionTitleClass) }) {
                Text(distribution.title)
            }
            Div(attrs = { classes(FlotViewStyleSheet.dashboardDistributionSummaryClass) }) {
                Text(distribution.summary)
            }
        }
        Div(attrs = {
            classes(FlotViewStyleSheet.dashboardDistributionTrackClass)
            attr("role", "img")
            attr(
                "aria-label",
                distribution.segments.joinToString { "${it.label}: ${it.percent}%" },
            )
        }) {
            distribution.segments.forEach { segment ->
                Div(attrs = {
                    classes(FlotViewStyleSheet.dashboardDistributionSegmentClass)
                    style {
                        property("flex-basis", "0")
                        property("flex-grow", segment.percent)
                    }
                    attr("title", "${segment.label}: ${segment.percent}%")
                })
            }
        }
        Div(attrs = { classes(FlotViewStyleSheet.dashboardDistributionLegendClass) }) {
            distribution.segments.forEach { segment ->
                Div(attrs = {
                    classes(FlotViewStyleSheet.dashboardDistributionLegendItemClass)
                }) {
                    Span(attrs = {
                        classes(FlotViewStyleSheet.dashboardDistributionSwatchClass)
                        attr("aria-hidden", "true")
                    })
                    Text("${segment.label} · ${segment.percent}%")
                }
            }
        }
    }
}

/** CSS class corresponding to this dashboard tone. */
private val FlotViewTone.dashboardToneClass: String
    get() = when (this) {
        FlotViewTone.Neutral -> FlotViewStyleSheet.toneNeutralClass
        FlotViewTone.Info -> FlotViewStyleSheet.toneInfoClass
        FlotViewTone.Success -> FlotViewStyleSheet.toneSuccessClass
        FlotViewTone.Warning -> FlotViewStyleSheet.toneWarningClass
        FlotViewTone.Error -> FlotViewStyleSheet.toneErrorClass
    }

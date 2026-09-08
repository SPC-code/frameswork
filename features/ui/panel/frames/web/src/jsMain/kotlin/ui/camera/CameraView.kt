package space.kscience.frameswork.features.ui.panel.frames.ui.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import dev.inmo.micro_utils.colors.common.HEXAColor
import dev.inmo.micro_utils.colors.dimgray
import dev.inmo.micro_utils.common.compose.tagClasses
import dev.inmo.micro_utils.common.fixed
import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.compose.StyleSheetsAggregator
import dev.inmo.micro_utils.meta.MetaContainer
import dev.inmo.micro_utils.strings.translation
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.mvvm.compose.ComposeView
import korlibs.time.DateTime
import korlibs.time.milliseconds
import korlibs.time.millisecondsLong
import korlibs.time.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.web.css.AlignItems
import org.jetbrains.compose.web.css.DisplayStyle
import org.jetbrains.compose.web.css.JustifyContent
import org.jetbrains.compose.web.css.Position
import org.jetbrains.compose.web.css.StyleSheet
import org.jetbrains.compose.web.css.alignItems
import org.jetbrains.compose.web.css.backgroundColor
import org.jetbrains.compose.web.css.bottom
import org.jetbrains.compose.web.css.display
import org.jetbrains.compose.web.css.height
import org.jetbrains.compose.web.css.justifyContent
import org.jetbrains.compose.web.css.left
import org.jetbrains.compose.web.css.percent
import org.jetbrains.compose.web.css.position
import org.jetbrains.compose.web.css.px
import org.jetbrains.compose.web.css.right
import org.jetbrains.compose.web.css.top
import org.jetbrains.compose.web.css.width
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Img
import org.jetbrains.compose.web.dom.Span
import org.jetbrains.compose.web.dom.Text
import org.koin.core.parameter.parametersOf
import space.kscience.frameswork.features.common.web.models.ViewConfig
import org.koin.core.component.inject
import space.kscience.frameswork.features.common.web.ui.FlotViewDashboardVideoSurface
import space.kscience.frameswork.features.common.web.ui.FlotViewTone
import space.kscience.frameswork.features.common.web.utils.cssRGBA
import space.kscience.frameswork.features.frames.common.models.FrameSourceHeight
import space.kscience.frameswork.features.frames.common.models.FrameSourceWidth
import space.kscience.frameswork.features.ui.panel.frames.CameraStrings
import kotlin.io.encoding.Base64
import kotlin.math.roundToInt

class CameraView(
    chain: NavigationChain<ViewConfig>,
    config: CameraViewConfig,
) : ComposeView<CameraViewConfig, ViewConfig, CameraViewModel>(config, chain) {
    override val viewModel: CameraViewModel by inject(mode = LazyThreadSafetyMode.SYNCHRONIZED) { parametersOf(this@CameraView) }

    private object CameraViewStyleSheet : StyleSheet() {
        val container by style {
            width(100.percent)
            height(100.percent)
            position(Position.Relative)
        }
        val image by style {
            width(100.percent)
            height(100.percent)
            property("object-fit", "contain")
        }
        val loader by style {
            position(Position.Absolute)
            display(DisplayStyle.Flex)
            justifyContent(JustifyContent.Center)
            alignItems(AlignItems.Center)
            top(0.px)
            right(0.px)
            bottom(0.px)
            left(0.px)
            backgroundColor(HEXAColor.dimgray.copy(aOfOne = 0.5f).cssRGBA)
        }

        init {
            StyleSheetsAggregator.addStyleSheet(this)
        }
    }

    @Composable
    override fun onDraw() {
        super.onDraw()

        val convertedState = remember { MutableRedeliverStateFlow<String?>(null) }
        val showLoading = remember { mutableStateOf(true) }
        val metaState = remember { mutableStateOf<MetaContainer?>(null) }
        val framesState = remember { mutableStateOf<Double>(0.0) }
        val bottomRightLabelState = remember { mutableStateOf<String?>(null) }
        LaunchedEffect(Unit) {
            withContext(Dispatchers.Default) {
                var latestFrameDateTime = DateTime.now()
                var framesZerrifierJob: Job? = null
                viewModel.frames.collect {
                    framesZerrifierJob ?.cancel()
                    val base64Encoded = Base64.encode(it.bytes)
                    val now = DateTime.now()
                    val timeDiff = (now - latestFrameDateTime)
                    latestFrameDateTime = now
                    val framesPerSecond = 1.seconds / timeDiff

                    val width = it.meta.get(FrameSourceWidth)
                    val height = it.meta.get(FrameSourceHeight)
                    val widthHeightPrefix = if (width != null && height != null) {
                        "$width×$height"
                    } else {
                        null
                    }
                    val bottomRightLabel = buildString {
                        widthHeightPrefix ?.let(::append)
                        if (widthHeightPrefix != null) {
                            append(" · ")
                        }
                        append(framesPerSecond.roundToInt().toString().padStart(3, ' '))
                        append(" ${CameraStrings.fpsSuffix.translation()}")
                    }

                    convertedState.value = "data:image/jpg;base64,$base64Encoded"
                    metaState.value = it.meta
                    framesState.value = framesPerSecond
                    bottomRightLabelState.value = bottomRightLabel

                    framesZerrifierJob = launch {
                        delay(1.seconds)
                        framesState.value = 0.0
                    }
                }
            }
        }
        LaunchedEffect(Unit) {
            var job: Job? = null
            convertedState.collect {
                if (it != null) {
                    showLoading.value = false
                }
                job ?.cancel()
                job = launch {
                    delay(2000L)
                    showLoading.value = true
                }
            }
        }
        val isLive = framesState.value > 0.0

        FlotViewDashboardVideoSurface(
            {
                val livePrefix = if (isLive) {
                    CameraStrings.live.translation().uppercase()
                } else {
                    CameraStrings.offline.translation().uppercase()
                }
                Text(
                    "$livePrefix · ${config.framesFlowFeatureId.processorName}/${config.framesFlowFeatureId.framesSourceId.string}"
                )
            },
            liveTone = if (isLive) {
                FlotViewTone.Error
            } else {
                FlotViewTone.Neutral
            },
            bottomRightLabel = bottomRightLabelState.value ?.let {
                {
                    Text(it)
                }
            }
        ) {
            Img(
                src = convertedState.collectAsState().value ?: "",
                attrs = {
                    classes(CameraViewStyleSheet.image)
                }
            )
        }
//        Div({
//            classes(CameraViewStyleSheet.container)
//        }) {
//            Img(
//                src = convertedState.collectAsState().value ?: "",
//                attrs = {
//                    classes(CameraViewStyleSheet.image)
//                }
//            )
//            if (showLoading.value) {
//                Div({
//                    classes(CameraViewStyleSheet.loader)
//                }) {
//                    Div({
//                        classes("spinner-border", "text-light")
//                        attr("role", "status")
//                    }) {
//                        Span(tagClasses("visually-hidden")) {}
//                    }
//                }
//            }
//        }
    }
}
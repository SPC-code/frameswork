package space.kscience.frameswork.features.ui.panel.ui.sample

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import dev.inmo.micro_utils.colors.black
import dev.inmo.micro_utils.colors.common.HEXAColor
import dev.inmo.micro_utils.common.compose.tagClasses
import dev.inmo.micro_utils.strings.translation
import org.jetbrains.compose.web.attributes.InputType
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Input
import org.jetbrains.compose.web.dom.Label
import org.jetbrains.compose.web.dom.Text
import space.kscience.frameswork.features.common.web.models.ViewConfig
import space.kscience.frameswork.features.ui.panel.PanelStrings
import space.kscience.frameswork.features.ui.panel.ui.PanelViewConfigProvider
import space.kscience.frameswork.features.ui.sample.ui.SampleViewConfig

/**
 * Built-in editor for creating a sample panel item with text and a background color.
 *
 * The editor reports `null` until both a non-blank text value and a color have been supplied.
 */
object SamplePanelViewConfigProvider : PanelViewConfigProvider {
    /** Localized title used for this provider in the panel's provider selector. */
    override val title: String
        @Composable get() = PanelStrings.panelSampleTitle.translation()

    /**
     * Draws text and color inputs and reports a [SampleViewConfig] once both values are valid.
     *
     * @param reportState callback that receives the current configuration or `null` while either
     * input is incomplete.
     */
    @Composable
    override fun Draw(reportState: (ViewConfig?) -> Unit) {
        val backgroundColorState = remember { mutableStateOf<HEXAColor?>(null) }
        val textState = remember { mutableStateOf<String>("") }

        LaunchedEffect(backgroundColorState.value, textState.value) {
            val config = let {
                SampleViewConfig(
                    backgroundColor = backgroundColorState.value ?: return@let null,
                    text = textState.value.takeIf { it.isNotBlank() } ?: return@let null
                )
            }
            reportState(config)
        }

        Div(tagClasses("mb-3")) {
            Label { Text(PanelStrings.sampleConfigTextLabel.translation()) }
            Input(
                InputType.Text
            ) {
                value(textState.value)
                onInput {
                    textState.value = it.value
                }
            }
        }
        Div(tagClasses("mb-3")) {
            Label { Text(PanelStrings.sampleConfigBackgroundColorLabel.translation()) }
            Input(
                InputType.Color
            ) {
                value((backgroundColorState.value ?: HEXAColor.black).rgba)
                onInput {
                    backgroundColorState.value = HEXAColor.parseStringColor(it.value)
                }
            }
        }
    }
}

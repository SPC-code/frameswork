package space.kscience.frameswork.features.ui.sample.ui

import dev.inmo.micro_utils.colors.common.HEXAColor
import kotlinx.serialization.Serializable
import space.kscience.frameswork.features.common.web.models.ViewConfig

/**
 * Serializable configuration that identifies and configures the sample navigation view.
 *
 * The current JavaScript `SampleView` renders the shared FlotView reference dashboard and does not
 * yet apply these presentation values.
 */
@Serializable
data class SampleViewConfig(
    /** Optional background colour requested for the sample view. */
    val backgroundColor: HEXAColor? = null,
    /** Text requested for the sample view; defaults to `Hello, world!`. */
    val text: String = "Hello, world!"
) : ViewConfig {

}

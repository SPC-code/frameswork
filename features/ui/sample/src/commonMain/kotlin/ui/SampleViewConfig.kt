package space.kscience.frameswork.features.ui.sample.ui

import dev.inmo.micro_utils.colors.common.HEXAColor
import kotlinx.serialization.Serializable
import space.kscience.frameswork.features.common.web.models.ViewConfig

@Serializable
data class SampleViewConfig(
    val backgroundColor: HEXAColor? = null,
    val text: String = "Hello, world!"
) : ViewConfig {

}
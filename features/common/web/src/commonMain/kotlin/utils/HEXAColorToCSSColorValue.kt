package space.kscience.frameswork.features.common.web.utils

import dev.inmo.micro_utils.colors.common.HEXAColor
import org.jetbrains.compose.web.css.CSSColorValue

val HEXAColor.cssRGBA: CSSColorValue
    get() = rgba.unsafeCast<CSSColorValue>()

package space.kscience.frameswork.features.common.web.utils

import dev.inmo.micro_utils.colors.common.HEXAColor
import org.jetbrains.compose.web.css.CSSColorValue

/**
 * Exposes this colour's RGBA representation as a Compose Web CSS colour value.
 *
 * The underlying `rgba` value is supplied by the JavaScript colour implementation and is cast
 * without allocating a wrapper.
 */
val HEXAColor.cssRGBA: CSSColorValue
    get() = rgba.unsafeCast<CSSColorValue>()

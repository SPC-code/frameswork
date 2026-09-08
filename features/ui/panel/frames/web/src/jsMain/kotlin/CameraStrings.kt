package space.kscience.frameswork.features.ui.panel.frames

import dev.inmo.micro_utils.language_codes.IetfLang
import dev.inmo.micro_utils.strings.buildStringResource

object CameraStrings {
    val live = buildStringResource("Live") {
        IetfLang.Russian("Онлайн")
    }
    val offline = buildStringResource("Offline") {
        IetfLang.Russian("Офлайн")
    }
    val fpsSuffix = buildStringResource("fps") {
        IetfLang.Russian("к/с")
    }
}
package space.kscience.frameswork.features.ui.panel.frames

import dev.inmo.micro_utils.language_codes.IetfLang
import dev.inmo.micro_utils.strings.buildStringResource

/** Localized status labels rendered over the camera image. */
object CameraStrings {
    /** Label used while frames are arriving. */
    val live = buildStringResource("Live") {
        IetfLang.Russian("Онлайн")
    }

    /** Label used when no recent frame has arrived. */
    val offline = buildStringResource("Offline") {
        IetfLang.Russian("Офлайн")
    }

    /** Unit suffix appended to the calculated frame rate. */
    val fpsSuffix = buildStringResource("fps") {
        IetfLang.Russian("к/с")
    }
}

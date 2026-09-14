package space.kscience.frameswork.features.ui.sample

import dev.inmo.micro_utils.language_codes.IetfLang
import dev.inmo.micro_utils.strings.buildStringResource

/** Localized string resources owned by the sample UI module. */
object SampleStrings {
    /** The sample-screen label, with English as the default and a Russian translation. */
    val sample = buildStringResource("Sample") {
        IetfLang.Russian("Пример")
    }
}

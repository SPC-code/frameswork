package space.kscience.frameswork.features.ui.panel.frames

import dev.inmo.micro_utils.language_codes.IetfLang
import dev.inmo.micro_utils.strings.buildStringResource
import space.kscience.frameswork.features.common.web.CommonResources

/** Localized labels shared by the camera view and its configuration UI. */
object CommonPanelStrings {
    /** Title shown for the raw camera-stream view. */
    val cameraViewTitle = buildStringResource("Raw camera video") {
        IetfLang.Russian("Видео с камеры")
    }

    /** Placeholder shown before a frame processor is selected. */
    val noProcessorSelected = buildStringResource("-- No processor selected --") {
        IetfLang.Russian("-- Процессор не выбран --")
    }

    /** Placeholder shown before a camera source is selected. */
    val noCameraSelected = buildStringResource("-- No camera selected --") {
        IetfLang.Russian("-- Камера не выбрана --")
    }

    /** Placeholder available to UIs that require a parameter-info selection. */
    val noParameterInfoSelected = buildStringResource("-- No parameter info selected --") {
        IetfLang.Russian("-- Параметр не выбран --")
    }
}

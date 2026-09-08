package space.kscience.frameswork.features.ui.panel.frames

import dev.inmo.micro_utils.language_codes.IetfLang
import dev.inmo.micro_utils.strings.buildStringResource
import space.kscience.frameswork.features.common.web.CommonResources

object CommonPanelStrings {
    val cameraViewTitle = buildStringResource("Raw camera video") {
        IetfLang.Russian("Видео с камеры")
    }
    val noProcessorSelected = buildStringResource("-- No processor selected --") {
        IetfLang.Russian("-- Процессор не выбран --")
    }
    val noCameraSelected = buildStringResource("-- No camera selected --") {
        IetfLang.Russian("-- Камера не выбрана --")
    }
    val noParameterInfoSelected = buildStringResource("-- No parameter info selected --") {
        IetfLang.Russian("-- Параметр не выбран --")
    }
}
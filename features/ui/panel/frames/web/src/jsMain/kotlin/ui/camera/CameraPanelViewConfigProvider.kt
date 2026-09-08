package space.kscience.frameswork.features.ui.panel.frames.ui.camera

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import dev.inmo.micro_utils.strings.translation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.jetbrains.compose.web.attributes.ButtonType
import org.jetbrains.compose.web.attributes.type
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Li
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Ul
import space.kscience.frameswork.features.frames.common.models.FramesSourceId
import space.kscience.frameswork.features.common.web.models.ViewConfig
import space.kscience.frameswork.features.ui.panel.frames.CommonPanelStrings
import space.kscience.frameswork.features.ui.panel.frames.common.features.FramesFlowFeatureId
import space.kscience.frameswork.features.ui.panel.ui.PanelViewConfigProvider

class CameraPanelViewConfigProvider(
    private val model: CameraModel
) : PanelViewConfigProvider {
    override val title: String
        @Composable get() = CommonPanelStrings.cameraViewTitle.translation()

    @Composable
    override fun Draw(reportState: (ViewConfig?) -> Unit) {
        val availableProcessorsNames = remember { mutableStateOf<Set<String>?>(emptySet()) }
        LaunchedEffect(Unit) {
            while (isActive) {
                withContext(Dispatchers.Default) {
                    val processorsNames = model.getAvailableProcessors()
                    availableProcessorsNames.value = processorsNames
                }
                delay(1000L)
            }
        }
        val selectedProcessor = remember { mutableStateOf<String?>(null) }
        val selectedCamera = remember { mutableStateOf<FramesSourceId?>(null) }
        LaunchedEffect(selectedProcessor.value, selectedCamera.value) {
            val selectedCameraId = selectedCamera.value ?: return@LaunchedEffect
            val selectedProcessorName = selectedProcessor.value ?: return@LaunchedEffect
            reportState(
                CameraViewConfig(
                    FramesFlowFeatureId(
                        selectedProcessorName,
                        selectedCameraId
                    )
                )
            )
        }

        val availableCamerasIds = remember { mutableStateOf<Set<FramesSourceId>?>(emptySet()) }
        LaunchedEffect(selectedProcessor.value) {
            val selectedProcessorValue = selectedProcessor.value
            if (selectedProcessorValue == null) {
                availableCamerasIds.value = null
            } else {
                while (isActive) {
                    withContext(Dispatchers.Default) {
                        val cameras_ids = model.getAvailableCameras(selectedProcessorValue)
                        availableCamerasIds.value = cameras_ids
                    }
                    delay(1000L)
                }
            }
        }

        Div(attrs = { classes("dropdown") }) {
            Button(attrs = {
                classes("btn", "btn-secondary", "dropdown-toggle")
                type(ButtonType.Button)
                attr("data-bs-toggle", "dropdown")
                attr("aria-expanded", "false")
            }) {
                Text(selectedProcessor.value ?: CommonPanelStrings.noProcessorSelected.translation())
            }
            Ul(attrs = { classes("dropdown-menu") }) {
                availableProcessorsNames.value ?.forEach {
                    Li {
                        A(
                            href = "#",
                            attrs = {
                                classes("dropdown-item")
                                onClick { _ ->
                                    selectedProcessor.value = it
                                }
                            }
                        ) {
                            Text(it)
                        }
                    }
                }
            }
        }

        Div(attrs = { classes("dropdown") }) {
            Button(attrs = {
                classes("btn", "btn-secondary", "dropdown-toggle")
                type(ButtonType.Button)
                attr("data-bs-toggle", "dropdown")
                attr("aria-expanded", "false")
            }) {
                Text(selectedCamera.value ?.string ?: CommonPanelStrings.noCameraSelected.translation())
            }
            Ul(attrs = { classes("dropdown-menu") }) {
                availableCamerasIds.value ?.forEach {
                    Li {
                        A(
                            href = "#",
                            attrs = {
                                classes("dropdown-item")
                                onClick { _ ->
                                    selectedCamera.value = it
                                }
                            }
                        ) {
                            Text(it.string)
                        }
                    }
                }
            }
        }
    }
}
package space.kscience.frameswork.features.ui.panel.frames.features

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.sample
import space.kscience.frameswork.features.frames.common.models.ByteArrayFrameData
import space.kscience.frameswork.features.frames.common.models.FramesSourceId
import space.kscience.frameswork.features.ui.panel.frames.common.features.FramesDataInfoFeature
import kotlin.time.Duration

open class GetFramesProxyFramesDataInfoFeature(
    private val original: FramesDataInfoFeature,
    private val mapper: (Flow<ByteArrayFrameData>) -> Flow<ByteArrayFrameData>
) : FramesDataInfoFeature by original {
    override fun getFramesFlow(
        processorName: String,
        id: FramesSourceId
    ): Flow<ByteArrayFrameData> {
        return mapper(original.getFramesFlow(processorName, id))
    }
}

fun GetFramesBackPressurePanelCameraInfoFeature(
    original: FramesDataInfoFeature,
    backPressureDelay: Duration,
) = GetFramesProxyFramesDataInfoFeature(
    original = original,
    mapper = {
        it.sample(backPressureDelay)
    }
)

package space.kscience.frameswork.features.ui.panel.frames.features

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.sample
import space.kscience.frameswork.features.frames.common.models.ByteArrayFrameData
import space.kscience.frameswork.features.frames.common.models.FramesSourceId
import space.kscience.frameswork.features.ui.panel.frames.common.features.FramesDataInfoFeature
import kotlin.time.Duration

/**
 * Decorates [original] by transforming each frame flow with [mapper].
 *
 * Metadata operations are delegated unchanged. The mapper is applied each time [getFramesFlow] is
 * called, so it can add rate limiting, buffering, or another flow operator without replacing the
 * metadata implementation.
 *
 * @param original feature that supplies metadata and the source frame flows.
 * @param mapper transformation applied to every requested frame flow.
 */
open class GetFramesProxyFramesDataInfoFeature(
    private val original: FramesDataInfoFeature,
    private val mapper: (Flow<ByteArrayFrameData>) -> Flow<ByteArrayFrameData>
) : FramesDataInfoFeature by original {
    /**
     * Returns [original]'s flow for [processorName] and [id] after applying the configured mapper.
     */
    override fun getFramesFlow(
        processorName: String,
        id: FramesSourceId
    ): Flow<ByteArrayFrameData> {
        return mapper(original.getFramesFlow(processorName, id))
    }
}

/**
 * Creates a frame-flow decorator that samples the latest frame once per [backPressureDelay].
 *
 * Processor and source discovery are delegated directly to [original]. Sampling can discard
 * intermediate frames when the producer is faster than the configured interval.
 *
 * @param original feature whose frame flows should be sampled.
 * @param backPressureDelay interval between sampled frame emissions.
 * @return a proxy that applies `Flow.sample` to every frame flow.
 */
fun GetFramesBackPressurePanelCameraInfoFeature(
    original: FramesDataInfoFeature,
    backPressureDelay: Duration,
) = GetFramesProxyFramesDataInfoFeature(
    original = original,
    mapper = {
        it.sample(backPressureDelay)
    }
)

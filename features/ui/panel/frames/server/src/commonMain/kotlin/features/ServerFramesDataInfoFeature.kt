package space.kscience.frameswork.features.ui.panel.frames.server.features

import korlibs.time.DateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import space.kscience.frameswork.features.frames.common.models.ByteArrayFrameData
import space.kscience.frameswork.features.frames.common.models.FrameReceiveTimestamp
import space.kscience.frameswork.features.frames.common.models.FramesSourceId
import space.kscience.frameswork.features.frames.common.services.FramesSourcesCollector
import space.kscience.frameswork.features.processor.server.ProcessorsContainer
import space.kscience.frameswork.features.ui.panel.frames.common.features.FramesDataInfoFeature

/**
 * Server-side [FramesDataInfoFeature] backed by the processors in [ProcessorsContainer].
 *
 * Processor and source queries return snapshots of the container's current state. Frame flows are
 * cold: the requested processor and its persistent source flow are resolved when collection starts.
 * Non-byte-backed frames are converted to [ByteArrayFrameData], and frames without a
 * [FrameReceiveTimestamp] or with a timestamp older than the last emitted frame are discarded.
 * Equal timestamps are retained.
 *
 * @property processorsContainer container that owns the processors queried and streamed by this
 * feature.
 * @property framesSourcesCollector frame-source registry required by the server feature wiring. The
 * current implementation retains this dependency but obtains source snapshots from each processor.
 */
open class ServerFramesDataInfoFeature(
    private val processorsContainer: ProcessorsContainer,
    private val framesSourcesCollector: FramesSourcesCollector,
) : FramesDataInfoFeature {
    /** Returns the current processor names reported by [processorsContainer]. */
    override suspend fun getAvailableProcessors(): Set<String> {
        return processorsContainer.availableProcessors()
    }

    /**
     * Returns the current frame-source snapshot for [processorName], or `null` if no such processor
     * is registered.
     *
     * @param processorName name of the processor whose sources should be read.
     * @return the processor's current source identifiers, or `null` for an unknown processor.
     */
    override suspend fun getAvailableFramesSources(processorName: String): Set<FramesSourceId>? {
        val processor = processorsContainer.getProcessor(processorName) ?: return null
        return processor.sourcesListUpdatesFlow.value
    }

    /**
     * Returns a cold flow of timestamped, byte-backed frames for source [id] on [processorName].
     *
     * Collection completes without values when the processor is unknown. For a known processor, its
     * persistent source flow remains quiet while [id] is unavailable and follows later source
     * additions or replacements. Frames without [FrameReceiveTimestamp] are skipped. After the
     * first emitted frame, a frame is skipped only when its timestamp is older than the preceding
     * emitted timestamp; duplicate timestamps are allowed.
     *
     * @param processorName name of the processor that should produce the frames.
     * @param id identifier of the source to stream from that processor.
     * @return a cold flow of filtered [ByteArrayFrameData] values.
     */
    override fun getFramesFlow(processorName: String, id: FramesSourceId): Flow<ByteArrayFrameData> {
        return flow {
            val persistentFlow = processorsContainer.getProcessor(processorName) ?.allocatePersistentFramesFlow(id)
            var latestFrameDateTime = DateTime(0)
            persistentFlow
                ?.map {
                    if (it is ByteArrayFrameData) {
                        it
                    } else {
                        ByteArrayFrameData(
                            it.toByteArray(),
                            it.meta
                        )
                    }
                }
                ?.filter {
                    val dateTime = it.meta[FrameReceiveTimestamp]
                    if (dateTime == null || dateTime < latestFrameDateTime) {
                        false
                    } else {
                        latestFrameDateTime = dateTime
                        true
                    }
                }
                ?.collect(this)
        }
    }
}

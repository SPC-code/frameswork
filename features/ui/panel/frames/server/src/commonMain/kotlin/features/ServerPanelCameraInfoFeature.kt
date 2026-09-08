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
import space.kscience.frameswork.features.ui.panel.frames.common.features.PanelCameraInfoFeature

/**
 * Implementation of [PanelCameraInfoFeature] providing details about available processors,
 * frame sources, and their associated metadata. Additionally, this class offers access to
 * real-time frame data streams through a combination of processor names and frame source identifiers.
 *
 * @constructor Creates an instance of [ServerPanelCameraInfoFeature].
 * @param processorsContainer Container managing frame processors and their configurations.
 * @param frameSources Map associating frame source identifiers with their configurations.
 */
class ServerPanelCameraInfoFeature(
    private val processorsContainer: ProcessorsContainer,
    private val framesSourcesCollector: FramesSourcesCollector,
) : PanelCameraInfoFeature {
    /**
     * Returns the current processor names reported by [processorsContainer].
     */
    override suspend fun getAvailableProcessors(): Set<String> {
        return processorsContainer.availableProcessors()
    }

    /**
     * Returns the current frame-source snapshot for [processorName], or `null` if no such processor
     * is registered.
     */
    override suspend fun getAvailableFramesSources(processorName: String): Set<FramesSourceId>? {
        val processor = processorsContainer.getProcessor(processorName) ?: return null
        return processor.sourcesListUpdatesFlow.value
    }

    /**
     * Returns the identifiers present in [frameSources].
     *
     * This configuration-backed set can omit sources that are exposed by a processor but have no
     * parameter configuration.
     */
    override suspend fun getAvailableFramesSourcesWithParametersInfo(): Set<FramesSourceId> {
        // TODO::FIX
        return framesSourcesCollector.framesSourcesIdsListUpdatesFlow.value // THIS LIST CAN BE UNCOMPLETE
    }

    /**
     * Retrieves a flow of byte-array-based frame data, filtered and mapped according to the specified
     * processor and frame source identifier.
     *
     * @param processorName The name of the processor to retrieve frames from.
     * @param id The identifier of the frame source associated with the processor.
     * @return A flow emitting instances of [ByteArrayFrameData], filtered based on their metadata
     *         timestamps to ensure only the new/latest frames are received.
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

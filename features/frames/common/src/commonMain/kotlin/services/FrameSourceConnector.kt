package space.kscience.frameswork.features.frames.common.services

import space.kscience.frameswork.features.frames.common.models.FrameSourceConnectorConfig
import space.kscience.frameswork.features.frames.common.models.FramesSourceId
import space.kscience.frameswork.features.frames.common.models.FramesSource

/**
 * A frame source with an identifier and a reproducible connector configuration.
 */
interface FrameSourceConnector : FramesSource {
    /** Identifier under which this connector is stored in source registries. */
    val id: FramesSourceId

    /**
     * Captures the settings required to recreate this connector.
     *
     * @return a configuration that creates an equivalent connector.
     */
    fun createConfig(): FrameSourceConnectorConfig
}

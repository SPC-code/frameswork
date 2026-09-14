package space.kscience.frameswork.features.frames.common.models

import space.kscience.frameswork.features.frames.common.services.FrameSourceConnector

/**
 * Serializable-style configuration capable of constructing a frame-source connector.
 *
 * Implementations define the concrete connector type and any transport-specific settings.
 */
interface FrameSourceConnectorConfig {
    /**
     * Creates a connector described by this configuration.
     *
     * @return a newly configured [FrameSourceConnector].
     */
    fun createFramesSourceConnector(): FrameSourceConnector
}

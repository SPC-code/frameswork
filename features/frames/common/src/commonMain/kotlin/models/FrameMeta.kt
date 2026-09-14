package space.kscience.frameswork.features.frames.common.models

import dev.inmo.micro_utils.meta.MetaContainer
import korlibs.time.DateTime
import kotlinx.serialization.Serializable

/** Metadata key containing the stable identifier of the frame source. */
@Serializable
data object FramesSourceIdMeta : MetaContainer.Key<FramesSourceId>

/** Metadata key containing the frame width in pixels. */
@Serializable
data object FrameSourceWidth : MetaContainer.Key<Int>

/** Metadata key containing the frame height in pixels. */
@Serializable
data object FrameSourceHeight : MetaContainer.Key<Int>

/** Metadata key containing the time at which Frameswork received the frame. */
@Serializable
data object FrameReceiveTimestamp : MetaContainer.Key<DateTime>

/** Metadata key containing the source-provided presentation timestamp in microseconds. */
@Serializable
data object FrameSourceTimestampMicroseconds : MetaContainer.Key<Long>

package space.kscience.frameswork.features.frames.common.models

import dev.inmo.micro_utils.meta.MetaContainer
import korlibs.time.DateTime
import kotlinx.serialization.Serializable

@Serializable
data object FramesSourceIdMeta : MetaContainer.Key<FramesSourceId>

@Serializable
data object FrameSourceWidth : MetaContainer.Key<Int>

@Serializable
data object FrameSourceHeight : MetaContainer.Key<Int>

@Serializable
data object FrameReceiveTimestamp : MetaContainer.Key<DateTime>
@Serializable
data object FrameSourceTimestampMicroseconds : MetaContainer.Key<Long>

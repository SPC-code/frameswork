package space.kscience.frameswork.features.frames.common.models

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class FramesSourceId(val string: String)
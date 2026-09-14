package space.kscience.frameswork.features.frames.common.models

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

/**
 * Stable textual identifier of a [FramesSource].
 *
 * @property string identifier value used by collectors and serialized configurations.
 */
@Serializable
@JvmInline
value class FramesSourceId(val string: String)

package space.kscience.frameswork.features.frames.common.models

import dev.inmo.micro_utils.meta.MetaContainer
import dev.inmo.micro_utils.meta.buildMetaContainer
import space.kscience.frameswork.features.common.common.utils.modified

/**
 * Frame data whose payload is already represented as bytes.
 *
 * The payload is retained by reference; callers that require isolation must copy [bytes] themselves.
 *
 * @property bytes raw frame payload returned by [toByteArray].
 * @property meta metadata associated with the payload.
 */
class ByteArrayFrameData(
    val bytes: ByteArray,
    override val meta: MetaContainer
) : FrameData {
    /** Returns the original [bytes] instance without copying it. */
    override suspend fun toByteArray(): ByteArray {
        return bytes
    }

    /**
     * Creates a frame that shares [bytes] with this instance and contains the metadata changes from [block].
     *
     * @param block mutations applied to a copy of [meta].
     * @return a new [ByteArrayFrameData] with the modified metadata.
     */
    override fun copyWithModifiedMeta(block: MetaContainer.Builder.() -> Unit): FrameData = ByteArrayFrameData(
        bytes,
        meta.modified(block)
    )
}

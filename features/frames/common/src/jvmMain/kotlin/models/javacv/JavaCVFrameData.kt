package space.kscience.frameswork.features.frames.common.models.javacv

import dev.inmo.micro_utils.meta.MetaContainer
import org.bytedeco.javacv.Frame
import space.kscience.frameswork.features.common.common.utils.modified
import space.kscience.frameswork.features.frames.common.models.FrameData

/**
 * JVM frame backed by a JavaCV [Frame].
 *
 * @property frame JavaCV frame whose data buffer supplies the byte payload.
 * @property meta metadata associated with the frame.
 */
data class JavaCVFrameData(
    val frame: Frame,
    override val meta: MetaContainer
) : FrameData {
    /**
     * Returns the backing array of [Frame.data].
     *
     * @return the data buffer's backing byte array without copying it.
     * @throws UnsupportedOperationException when the buffer is not array-backed.
     */
    override suspend fun toByteArray(): ByteArray {
        return frame.data.array()
    }

    /**
     * Creates a frame that shares [frame] and contains the metadata changes from [block].
     *
     * @return a new [JavaCVFrameData] with modified metadata.
     */
    override fun copyWithModifiedMeta(block: MetaContainer.Builder.() -> Unit): FrameData = JavaCVFrameData(
        frame,
        meta.modified(block)
    )
}

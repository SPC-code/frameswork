package space.kscience.frameswork.features.frames.common.models

import dev.inmo.micro_utils.meta.MetaContainer

/**
 * A single frame payload together with metadata describing its source and capture properties.
 */
interface FrameData {
    /** Metadata associated with this frame. */
    val meta: MetaContainer

    /**
     * Converts the frame payload to bytes.
     *
     * Implementations may perform blocking or CPU-intensive conversion in an appropriate coroutine context.
     *
     * @return the encoded or raw bytes representing this frame.
     */
    suspend fun toByteArray(): ByteArray

    /**
     * Copies this frame while applying [block] to a mutable copy of its metadata.
     *
     * The underlying payload may be shared by the returned frame.
     *
     * @param block metadata changes to apply.
     * @return a frame with the same payload and modified metadata.
     */
    fun copyWithModifiedMeta(block: MetaContainer.Builder.() -> Unit): FrameData
}

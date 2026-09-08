package space.kscience.frameswork.features.frames.common.models

import dev.inmo.micro_utils.meta.MetaContainer
import dev.inmo.micro_utils.meta.buildMetaContainer
import space.kscience.frameswork.features.common.common.utils.modified

class ByteArrayFrameData(
    val bytes: ByteArray,
    override val meta: MetaContainer
) : FrameData {
    override suspend fun toByteArray(): ByteArray {
        return bytes
    }

    override fun copyWithModifiedMeta(block: MetaContainer.Builder.() -> Unit): FrameData = ByteArrayFrameData(
        bytes,
        meta.modified(block)
    )
}

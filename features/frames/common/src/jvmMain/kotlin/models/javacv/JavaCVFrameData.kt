package space.kscience.frameswork.features.frames.common.models.javacv

import dev.inmo.micro_utils.meta.MetaContainer
import org.bytedeco.javacv.Frame
import space.kscience.frameswork.features.common.common.utils.modified
import space.kscience.frameswork.features.frames.common.models.FrameData

data class JavaCVFrameData(
    val frame: Frame,
    override val meta: MetaContainer
) : FrameData {
    override suspend fun toByteArray(): ByteArray {
        return frame.data.array()
    }

    override fun copyWithModifiedMeta(block: MetaContainer.Builder.() -> Unit): FrameData = JavaCVFrameData(
        frame,
        meta.modified(block)
    )
}

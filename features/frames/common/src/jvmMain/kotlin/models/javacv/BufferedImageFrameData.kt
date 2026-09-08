package space.kscience.frameswork.features.frames.common.models.javacv

import dev.inmo.micro_utils.meta.MetaContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import space.kscience.frameswork.features.common.common.utils.modified
import space.kscience.frameswork.features.frames.common.models.FrameData
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

data class BufferedImageFrameData(
    val bufferedImage: BufferedImage,
    override val meta: MetaContainer
) : FrameData {
    override suspend fun toByteArray(): ByteArray {
        val outputStream = ByteArrayOutputStream()

        withContext(Dispatchers.IO) {
            ImageIO.write(bufferedImage, "jpg", outputStream)
        }
        return outputStream.toByteArray()
    }

    override fun copyWithModifiedMeta(block: MetaContainer.Builder.() -> Unit): FrameData = BufferedImageFrameData(
        bufferedImage,
        meta.modified(block)
    )
}

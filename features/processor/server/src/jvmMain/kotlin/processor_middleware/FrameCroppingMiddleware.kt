package space.kscience.frameswork.features.processor.server.processor_middleware

import com.sksamuel.scrimage.ImmutableImage
import kotlinx.serialization.Serializable
import space.kscience.frameswork.features.frames.common.models.FrameData
import space.kscience.frameswork.features.frames.common.models.javacv.BufferedImageFrameData
import space.kscience.frameswork.features.processor.common.services.FramesProcessorMiddleware
import java.awt.image.BufferedImage

class FrameCroppingMiddleware(
    private val cropParameter: CropData,
) : FramesProcessorMiddleware {
    @Serializable
    data class CropData(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int
    )
    class Factory(
        private val cropParameter: CropData,
        private val suffix: String
    ) : FramesProcessorMiddleware.Factory {
        override val id: FramesProcessorMiddleware.Factory.Id = FramesProcessorMiddleware.Factory.Id("crop_$suffix")

        override suspend fun createMiddleware(): FramesProcessorMiddleware {
            return FrameCroppingMiddleware(cropParameter)
        }
    }

    override suspend fun process(frame: FrameData): FrameData {
        val outputBufferedImages: BufferedImage? = when (frame) {
            is BufferedImageFrameData -> {
                frame.bufferedImage.getSubimage(
                    /* x = */ cropParameter.x,
                    /* y = */ cropParameter.y,
                    /* w = */ cropParameter.width,
                    /* h = */ cropParameter.height
                )
            }
            else -> {
                val image = ImmutableImage.loader().fromBytes(frame.toByteArray())

                val width = image.width
                val height = image.height
                image
                    .dropBottom(height - cropParameter.height - cropParameter.y)
                    .dropRight(width - cropParameter.width - cropParameter.x)
                    .dropLeft(cropParameter.x)
                    .dropTop(cropParameter.y)
                    .awt()
            }
        }

        if (outputBufferedImages == null) error("Unable to crop image for parameters $cropParameter")

        return BufferedImageFrameData(outputBufferedImages, frame.meta)
    }
}
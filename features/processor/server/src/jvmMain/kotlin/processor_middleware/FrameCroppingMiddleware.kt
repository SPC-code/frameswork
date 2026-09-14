package space.kscience.frameswork.features.processor.server.processor_middleware

import com.sksamuel.scrimage.ImmutableImage
import kotlinx.serialization.Serializable
import space.kscience.frameswork.features.frames.common.models.FrameData
import space.kscience.frameswork.features.frames.common.models.javacv.BufferedImageFrameData
import space.kscience.frameswork.features.processor.common.services.FramesProcessorMiddleware
import java.awt.image.BufferedImage

/**
 * Crops image frames to the rectangle described by [cropParameter].
 *
 * [BufferedImageFrameData] inputs are cropped directly. Other frame implementations are decoded
 * with Scrimage first. The result is always a [BufferedImageFrameData] and retains the input frame's
 * metadata.
 *
 * @param cropParameter crop rectangle measured from the image's top-left corner.
 */
class FrameCroppingMiddleware(
    private val cropParameter: CropData,
) : FramesProcessorMiddleware {
    /**
     * Defines a rectangular image crop.
     *
     * @property x horizontal offset of the rectangle's left edge, in pixels.
     * @property y vertical offset of the rectangle's top edge, in pixels.
     * @property width rectangle width in pixels.
     * @property height rectangle height in pixels.
     */
    @Serializable
    data class CropData(
        val x: Int,
        val y: Int,
        val width: Int,
        val height: Int
    )

    /**
     * Creates cropping middleware instances for one named crop configuration.
     *
     * @param cropParameter rectangle supplied to every created middleware.
     * @param suffix suffix used to form the factory identifier `crop_<suffix>`.
     */
    class Factory(
        private val cropParameter: CropData,
        private val suffix: String
    ) : FramesProcessorMiddleware.Factory {
        /** Identifier used by processor configurations to select this factory. */
        override val id: FramesProcessorMiddleware.Factory.Id = FramesProcessorMiddleware.Factory.Id("crop_$suffix")

        /** Creates a new cropping middleware with the configured rectangle. */
        override suspend fun createMiddleware(): FramesProcessorMiddleware {
            return FrameCroppingMiddleware(cropParameter)
        }
    }

    /**
     * Crops [frame] and preserves its metadata on the returned frame.
     *
     * The crop rectangle must be valid for the decoded image; invalid bounds or undecodable image
     * bytes cause the underlying image operation to fail.
     *
     * @param frame image frame to crop.
     * @return the cropped image as [BufferedImageFrameData].
     */
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

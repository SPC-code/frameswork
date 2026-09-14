package space.kscience.frameswork.features.processor.common.services.middlewares

import korlibs.time.DateTime
import space.kscience.frameswork.features.frames.common.models.FrameData
import space.kscience.frameswork.features.frames.common.models.javacv.BufferedImageFrameData
import space.kscience.frameswork.features.processor.common.services.FramesProcessorMiddleware
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.io.path.Path

/**
 * Saves JVM [BufferedImageFrameData] inputs as JPEG files and passes every frame through unchanged.
 *
 * The destination directory is created during construction. Frames of other implementations are not written.
 *
 * @param folderPath Destination directory, resolved according to normal JVM path rules.
 */
class BufferedImageSaverMiddleware(
    private val folderPath: String
) : FramesProcessorMiddleware {
    /**
     * Creates [BufferedImageSaverMiddleware] instances for [folderPath].
     *
     * @param folderPath Destination directory supplied to each created middleware.
     */
    class Factory(
        private val folderPath: String
    ) : FramesProcessorMiddleware.Factory {
        /** Stable configuration identifier for the image-saver middleware. */
        override val id: FramesProcessorMiddleware.Factory.Id = FramesProcessorMiddleware.Factory.Id("images_saver")

        /** Creates a saver that writes into the configured directory. */
        override suspend fun createMiddleware(): FramesProcessorMiddleware {
            return BufferedImageSaverMiddleware(folderPath)
        }
    }

    init {
        Files.createDirectories(Path(folderPath))
    }

    /**
     * Writes [frame] as a timestamp-named JPEG when it is a [BufferedImageFrameData].
     *
     * @param frame Frame to save or pass through.
     * @return The same [frame] instance.
     */
    override suspend fun process(frame: FrameData): FrameData {
        if (frame is BufferedImageFrameData) {
            val now = DateTime.now().format("HH-mm-ss_dd-MM-yyyy")
            ImageIO.write(
                frame.bufferedImage,
                "jpg",
                File(folderPath, "$now.jpg")
            )
        }

        return frame
    }
}

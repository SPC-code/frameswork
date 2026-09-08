package space.kscience.frameswork.features.processor.common.services.middlewares

import korlibs.time.DateTime
import space.kscience.frameswork.features.frames.common.models.FrameData
import space.kscience.frameswork.features.frames.common.models.javacv.BufferedImageFrameData
import space.kscience.frameswork.features.processor.common.services.FramesProcessorMiddleware
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.io.path.Path

class BufferedImageSaverMiddleware(
    private val folderPath: String
) : FramesProcessorMiddleware {
    class Factory(
        private val folderPath: String
    ) : FramesProcessorMiddleware.Factory {
        override val id: FramesProcessorMiddleware.Factory.Id = FramesProcessorMiddleware.Factory.Id("images_saver")

        override suspend fun createMiddleware(): FramesProcessorMiddleware {
            return BufferedImageSaverMiddleware(folderPath)
        }
    }

    init {
        Files.createDirectories(Path(folderPath))
    }

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
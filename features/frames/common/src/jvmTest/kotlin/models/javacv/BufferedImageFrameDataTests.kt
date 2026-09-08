package space.kscience.frameswork.features.frames.common.models.javacv

import dev.inmo.micro_utils.meta.buildMetaContainer
import kotlinx.coroutines.test.runTest
import space.kscience.frameswork.features.frames.common.models.FrameSourceHeight
import space.kscience.frameswork.features.frames.common.models.FrameSourceWidth
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class BufferedImageFrameDataTests {
    @Test
    fun toByteArrayProducesReadableJpegWithOriginalDimensions() = runTest {
        val image = BufferedImage(4, 3, BufferedImage.TYPE_INT_RGB).apply {
            val graphics = createGraphics()
            try {
                graphics.color = Color(20, 80, 160)
                graphics.fillRect(0, 0, width, height)
            } finally {
                graphics.dispose()
            }
        }
        val frame = BufferedImageFrameData(image, dimensions(4, 3))

        val encoded = frame.toByteArray()
        val decoded = assertNotNull(ImageIO.read(ByteArrayInputStream(encoded)))

        assertTrue(encoded.size > 4)
        assertEquals(0xFF.toByte(), encoded[0])
        assertEquals(0xD8.toByte(), encoded[1])
        assertEquals(4, decoded.width)
        assertEquals(3, decoded.height)
    }

    @Test
    fun copyWithModifiedMetaKeepsImageAndDoesNotChangeOriginalMeta() {
        val image = BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB)
        val original = BufferedImageFrameData(image, dimensions(2, 2))

        val copy = original.copyWithModifiedMeta {
            put(FrameSourceWidth, 8)
            put(FrameSourceHeight, 6)
        } as BufferedImageFrameData

        assertNotSame(original, copy)
        assertSame(image, copy.bufferedImage)
        assertEquals(2, original.meta[FrameSourceWidth])
        assertEquals(2, original.meta[FrameSourceHeight])
        assertEquals(8, copy.meta[FrameSourceWidth])
        assertEquals(6, copy.meta[FrameSourceHeight])
    }

    private fun dimensions(width: Int, height: Int) = buildMetaContainer {
        put(FrameSourceWidth, width)
        put(FrameSourceHeight, height)
    }
}

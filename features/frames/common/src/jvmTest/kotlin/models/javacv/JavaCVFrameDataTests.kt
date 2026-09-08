package space.kscience.frameswork.features.frames.common.models.javacv

import dev.inmo.micro_utils.meta.buildMetaContainer
import kotlinx.coroutines.test.runTest
import org.bytedeco.javacv.Frame
import space.kscience.frameswork.features.frames.common.models.FrameSourceHeight
import space.kscience.frameswork.features.frames.common.models.FrameSourceWidth
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class JavaCVFrameDataTests {
    @Test
    fun toByteArrayReturnsFrameDataPayload() = runTest {
        val payload = byteArrayOf(1, 0, -1, 127)
        val javaCvFrame = Frame().apply {
            data = ByteBuffer.wrap(payload)
        }
        val frame = JavaCVFrameData(javaCvFrame, dimensions(320, 240))

        assertSame(payload, frame.toByteArray())
    }

    @Test
    fun copyWithModifiedMetaKeepsFrameAndDoesNotChangeOriginalMeta() {
        val javaCvFrame = Frame()
        val original = JavaCVFrameData(javaCvFrame, dimensions(320, 240))

        val copy = original.copyWithModifiedMeta {
            put(FrameSourceWidth, 640)
            put(FrameSourceHeight, 480)
        } as JavaCVFrameData

        assertNotSame(original, copy)
        assertSame(javaCvFrame, copy.frame)
        assertEquals(320, original.meta[FrameSourceWidth])
        assertEquals(240, original.meta[FrameSourceHeight])
        assertEquals(640, copy.meta[FrameSourceWidth])
        assertEquals(480, copy.meta[FrameSourceHeight])
    }

    private fun dimensions(width: Int, height: Int) = buildMetaContainer {
        put(FrameSourceWidth, width)
        put(FrameSourceHeight, height)
    }
}

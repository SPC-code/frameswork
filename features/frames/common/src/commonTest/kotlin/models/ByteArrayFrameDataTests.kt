package space.kscience.frameswork.features.frames.common.models

import dev.inmo.micro_utils.meta.buildMetaContainer
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class ByteArrayFrameDataTests {
    @Test
    fun toByteArrayReturnsOriginalPayload() = runTest {
        val payload = byteArrayOf(0, 1, -1, 127)
        val frame = ByteArrayFrameData(payload, MetaContainerFixtures.withDimensions(320, 240))

        assertSame(payload, frame.toByteArray())
    }

    @Test
    fun copyWithModifiedMetaKeepsPayloadAndDoesNotChangeOriginalMeta() {
        val payload = byteArrayOf(1, 2, 3)
        val original = ByteArrayFrameData(payload, MetaContainerFixtures.withDimensions(320, 240))

        val copy = original.copyWithModifiedMeta {
            put(FrameSourceWidth, 640)
            put(FrameSourceHeight, 480)
        } as ByteArrayFrameData

        assertNotSame(original, copy)
        assertSame(payload, copy.bytes)
        assertEquals(320, original.meta[FrameSourceWidth])
        assertEquals(240, original.meta[FrameSourceHeight])
        assertEquals(640, copy.meta[FrameSourceWidth])
        assertEquals(480, copy.meta[FrameSourceHeight])
    }
}

internal object MetaContainerFixtures {
    fun withDimensions(width: Int, height: Int) = buildMetaContainer {
        put(FrameSourceWidth, width)
        put(FrameSourceHeight, height)
    }
}

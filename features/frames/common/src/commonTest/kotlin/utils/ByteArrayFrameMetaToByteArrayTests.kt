package space.kscience.frameswork.features.frames.common.utils

import dev.inmo.micro_utils.meta.MetaContainer
import dev.inmo.micro_utils.meta.buildMetaContainer
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import space.kscience.frameswork.features.common.common.utils.toInt
import space.kscience.frameswork.features.frames.common.models.ByteArrayFrameData
import space.kscience.frameswork.features.frames.common.models.FrameData
import space.kscience.frameswork.features.frames.common.models.FrameSourceHeight
import space.kscience.frameswork.features.frames.common.models.FrameSourceWidth
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class ByteArrayFrameMetaToByteArrayTests {
    private val json = Json {
        useArrayPolymorphism = true
        allowStructuredMapKeys = true
        serializersModule = SerializersModule {
            polymorphic(Any::class, Int::class, Int.serializer())
            polymorphic(MetaContainer.Key::class, FrameSourceWidth::class, FrameSourceWidth.serializer())
            polymorphic(MetaContainer.Key::class, FrameSourceHeight::class, FrameSourceHeight.serializer())
        }
    }

    @Test
    fun encodeUsesDocumentedLayoutAndConvertsPayloadOnce() = runTest {
        val payload = byteArrayOf(0, -1, 2, 127, -128)
        val meta = buildMetaContainer {
            put(FrameSourceWidth, 1920)
            put(FrameSourceHeight, 1080)
        }
        val frame = CountingFrameData(payload, meta)
        val expectedMetaBytes = json
            .encodeToString(MetaContainer.serializer(), meta)
            .encodeToByteArray()

        val encoded = frame.encodeToByteArray(json)
        val encodedMetaSize = encoded.copyOfRange(0, Int.SIZE_BYTES).toInt()

        assertContentEquals(
            byteArrayOf(
                (expectedMetaBytes.size ushr 24).toByte(),
                (expectedMetaBytes.size ushr 16).toByte(),
                (expectedMetaBytes.size ushr 8).toByte(),
                expectedMetaBytes.size.toByte(),
            ),
            encoded.copyOfRange(0, Int.SIZE_BYTES),
        )
        assertEquals(expectedMetaBytes.size, encodedMetaSize)
        assertContentEquals(
            expectedMetaBytes,
            encoded.copyOfRange(Int.SIZE_BYTES, Int.SIZE_BYTES + encodedMetaSize),
        )
        assertContentEquals(payload, encoded.copyOfRange(Int.SIZE_BYTES + encodedMetaSize, encoded.size))
        assertEquals(1, frame.conversionCount)
    }

    @Test
    fun encodedFrameRoundTripsMetaAndBinaryPayload() = runTest {
        val payload = byteArrayOf(12, 0, -45, 99)
        val meta = buildMetaContainer {
            put(FrameSourceWidth, 800)
            put(FrameSourceHeight, 600)
        }

        val decoded = ByteArrayFrameData(payload, meta)
            .encodeToByteArray(json)
            .decodeFrameData(json)

        assertContentEquals(payload, decoded.bytes)
        assertEquals(meta, decoded.meta)
    }

    @Test
    fun emptyPayloadAndMetaRoundTrip() = runTest {
        val decoded = ByteArrayFrameData(byteArrayOf(), MetaContainer.EMPTY)
            .encodeToByteArray(json)
            .decodeFrameData(json)

        assertContentEquals(byteArrayOf(), decoded.bytes)
        assertEquals(MetaContainer.EMPTY, decoded.meta)
    }

    private class CountingFrameData(
        private val payload: ByteArray,
        override val meta: MetaContainer,
    ) : FrameData {
        var conversionCount = 0
            private set

        override suspend fun toByteArray(): ByteArray {
            conversionCount++
            return payload
        }

        override fun copyWithModifiedMeta(block: MetaContainer.Builder.() -> Unit): FrameData =
            error("Copying is not used by these tests")
    }
}

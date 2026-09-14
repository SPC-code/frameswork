package space.kscience.frameswork.features.ui.panel.frames.server.features

import dev.inmo.micro_utils.meta.MetaContainer
import dev.inmo.micro_utils.meta.buildMetaContainer
import korlibs.time.DateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import space.kscience.frameswork.features.frames.common.models.ByteArrayFrameData
import space.kscience.frameswork.features.frames.common.models.FrameData
import space.kscience.frameswork.features.frames.common.models.FrameReceiveTimestamp
import space.kscience.frameswork.features.frames.common.models.FramesSourceId
import space.kscience.frameswork.features.frames.common.services.FramesCollector
import space.kscience.frameswork.features.frames.common.services.InMemoryFramesSourcesCollector
import space.kscience.frameswork.features.processor.server.ProcessorsContainer
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/** Verifies the query, conversion, and timestamp-filtering behavior of [ServerFramesDataInfoFeature]. */
@OptIn(ExperimentalCoroutinesApi::class)
class ServerFramesDataInfoFeatureTests {
    /** Verifies that processor queries expose registered names, source snapshots, and unknowns. */
    @Test
    fun processorQueriesExposeNamesAndCurrentSources() = runTest {
        val leftSource = FramesSourceId("left")
        val rightSource = FramesSourceId("right")
        val collector = TestFramesCollector(
            initialSources = setOf(leftSource, rightSource),
            frameFlows = mapOf(
                leftSource to emptyFlow(),
                rightSource to emptyFlow(),
            ),
        )
        val container = processorsContainer(
            scope = backgroundScope,
            collector = collector,
            processorNames = setOf("main", "diagnostic"),
        )
        val feature = ServerFramesDataInfoFeature(
            container,
            InMemoryFramesSourcesCollector(emptyList(), backgroundScope),
        )

        awaitSources(container, "main", setOf(leftSource, rightSource))
        awaitSources(container, "diagnostic", setOf(leftSource, rightSource))

        assertEquals(setOf("main", "diagnostic"), feature.getAvailableProcessors())
        assertEquals(setOf(leftSource, rightSource), feature.getAvailableFramesSources("main"))
        assertEquals(setOf(leftSource, rightSource), feature.getAvailableFramesSources("diagnostic"))
        assertEquals(null, feature.getAvailableFramesSources("missing"))
    }

    /** Verifies that requesting frames from an unknown processor produces an empty flow. */
    @Test
    fun framesFlowForUnknownProcessorCompletesWithoutValues() = runTest {
        val feature = ServerFramesDataInfoFeature(
            processorsContainer(backgroundScope, TestFramesCollector(), setOf("main")),
            InMemoryFramesSourcesCollector(emptyList(), backgroundScope),
        )

        assertTrue(feature.getFramesFlow("missing", FramesSourceId("camera")).toList().isEmpty())
    }

    /** Verifies byte conversion and rejection of missing or decreasing receive timestamps. */
    @Test
    fun framesFlowConvertsFramesAndDropsMissingOrOlderTimestamps() = runTest {
        val sourceId = FramesSourceId("camera")
        val frames = Channel<FrameData>(Channel.UNLIMITED)
        val collector = TestFramesCollector(
            initialSources = setOf(sourceId),
            frameFlows = mapOf(sourceId to frames.receiveAsFlow()),
        )
        val container = processorsContainer(backgroundScope, collector, setOf("main"))
        val feature = ServerFramesDataInfoFeature(
            container,
            InMemoryFramesSourcesCollector(emptyList(), backgroundScope),
        )
        awaitSources(container, "main", setOf(sourceId))

        val firstTimestamp = DateTime(100.0)
        val first = ByteArrayFrameData(byteArrayOf(1), meta(firstTimestamp))
        val sameTimestamp = ByteArrayFrameData(byteArrayOf(2), meta(firstTimestamp))
        val converted = ConvertibleFrameData(byteArrayOf(3), meta(DateTime(101.0)))
        val received = async {
            feature.getFramesFlow("main", sourceId).take(3).toList()
        }
        runCurrent()

        frames.send(ByteArrayFrameData(byteArrayOf(0), MetaContainer.EMPTY))
        runCurrent()
        frames.send(first)
        runCurrent()
        frames.send(ByteArrayFrameData(byteArrayOf(9), meta(DateTime(99.0))))
        runCurrent()
        frames.send(sameTimestamp)
        runCurrent()
        frames.send(converted)

        val result = received.await()
        assertEquals(3, result.size)
        assertSame(first, result[0])
        assertSame(sameTimestamp, result[1])
        assertContentEquals(byteArrayOf(3), result[2].bytes)
        assertSame(converted.meta, result[2].meta)
        assertEquals(1, converted.conversionCount)
    }

    /** Suspends until [processorName] reports exactly [expected] sources. */
    private suspend fun awaitSources(
        container: ProcessorsContainer,
        processorName: String,
        expected: Set<FramesSourceId>,
    ) {
        container.getProcessor(processorName)!!.sourcesListUpdatesFlow.first { it == expected }
    }

    /** Creates a processor container whose named processors all consume [collector]. */
    private fun processorsContainer(
        scope: CoroutineScope,
        collector: FramesCollector,
        processorNames: Set<String>,
    ): ProcessorsContainer = ProcessorsContainer(
        framesCollector = collector,
        scope = scope,
        processorsConfigs = processorNames.associateWith {
            ProcessorsContainer.ProcessorConfig(
                middlewares = emptyList(),
                parallelProcessing = 1,
                processingThrottlingMillis = 0,
            )
        },
        middlewaresFactories = emptyList(),
    )

    /** Builds frame metadata containing [timestamp] as its receive timestamp. */
    private fun meta(timestamp: DateTime): MetaContainer = buildMetaContainer {
        put(FrameReceiveTimestamp, timestamp)
    }

    /**
     * Minimal frame collector backed by fixed source flows.
     *
     * @param initialSources initial value exposed through [sourcesListUpdatesFlow].
     * @property frameFlows source-to-flow mapping returned by [allocateFramesFlow].
     */
    private class TestFramesCollector(
        initialSources: Set<FramesSourceId> = emptySet(),
        private val frameFlows: Map<FramesSourceId, Flow<FrameData>> = emptyMap(),
    ) : FramesCollector {
        /** Mutable source snapshot initialized from [initialSources]. */
        override val sourcesListUpdatesFlow = MutableStateFlow(initialSources)

        /** Returns the fixed frame flow assigned to [id], or `null` when none was assigned. */
        override fun allocateFramesFlow(id: FramesSourceId): Flow<FrameData>? = frameFlows[id]
    }

    /**
     * Non-byte-backed test frame that records payload conversions.
     *
     * @property bytes payload returned by [toByteArray].
     * @property meta metadata exposed with the frame.
     */
    private class ConvertibleFrameData(
        private val bytes: ByteArray,
        override val meta: MetaContainer,
    ) : FrameData {
        /** Number of times [toByteArray] has converted this frame. */
        var conversionCount: Int = 0
            private set

        /** Records one conversion and returns the configured payload. */
        override suspend fun toByteArray(): ByteArray {
            conversionCount++
            return bytes
        }

        /** Always fails because this test frame is not used for metadata transformations. */
        override fun copyWithModifiedMeta(block: MetaContainer.Builder.() -> Unit): FrameData {
            error("The test processor has no metadata-modifying middleware")
        }
    }
}

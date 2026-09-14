package space.kscience.frameswork.features.processor.common.services

import dev.inmo.micro_utils.meta.MetaContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import space.kscience.frameswork.features.frames.common.models.ByteArrayFrameData
import space.kscience.frameswork.features.frames.common.models.FrameData
import space.kscience.frameswork.features.frames.common.models.FramesSourceId
import space.kscience.frameswork.features.frames.common.services.FramesCollector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

/** Verifies middleware sequencing and source-flow behavior of [FramesProcessorService]. */
@OptIn(ExperimentalCoroutinesApi::class)
class FramesProcessorServiceTests {
    /** Verifies ordered middleware execution and recovery from a failed stage. */
    @Test
    fun processRunsMiddlewaresInOrderAndContinuesAfterFailure() = runTest {
        val input = frame(0)
        val afterFirst = frame(1)
        val result = frame(2)
        val receivedByMiddlewares = mutableListOf<FrameData>()
        val processor = processor(
            scope = backgroundScope,
            collector = TestFramesCollector(),
            middlewares = listOf(
                middleware {
                    receivedByMiddlewares += it
                    afterFirst
                },
                middleware {
                    receivedByMiddlewares += it
                    error("expected middleware failure")
                },
                middleware {
                    receivedByMiddlewares += it
                    result
                },
            ),
        )

        val processed = processor.process(input)

        assertSame(result, processed)
        assertEquals(listOf<FrameData>(input, afterFirst, afterFirst), receivedByMiddlewares)
    }

    /** Verifies that the collector API exposes processed frames only for currently available sources. */
    @Test
    fun collectorContractExposesProcessedFramesForAvailableSources() = runTest {
        val sourceId = FramesSourceId("camera")
        val sourceFrames = MutableSharedFlow<FrameData>()
        val collector = TestFramesCollector(mapOf(sourceId to sourceFrames))
        val processedFrame = frame(2)
        val processor = processor(
            scope = backgroundScope,
            collector = collector,
            middlewares = listOf(middleware { processedFrame }),
        )
        runCurrent()

        assertEquals(setOf(sourceId), processor.sourcesListUpdatesFlow.value)
        assertNull(processor.allocateFramesFlow(FramesSourceId("missing")))
        val output = assertNotNull(processor.allocateFramesFlow(sourceId))
        val received = async { output.first() }
        runCurrent()

        sourceFrames.emit(frame(1))
        runCurrent()

        assertSame(processedFrame, received.await())

        collector.setFlows(emptyMap())
        runCurrent()
        assertEquals(emptySet(), processor.sourcesListUpdatesFlow.value)
        assertNull(processor.allocateFramesFlow(sourceId))
    }

    /** Verifies that a persistent flow begins emitting when its requested source appears later. */
    @Test
    fun persistentFlowWaitsForARequestedSourceToBecomeAvailable() = runTest {
        val sourceId = FramesSourceId("late-camera")
        val sourceFrames = MutableSharedFlow<FrameData>()
        val collector = TestFramesCollector()
        val processor = processor(backgroundScope, collector)
        val expected = frame(7)
        val received = async {
            processor.allocatePersistentFramesFlow(sourceId).first()
        }
        runCurrent()

        collector.setFlows(mapOf(sourceId to sourceFrames))
        runCurrent()
        sourceFrames.emit(expected)
        runCurrent()

        assertSame(expected, received.await())
    }

    /** Creates a processor with deterministic single-worker settings for a test. */
    private fun processor(
        scope: CoroutineScope,
        collector: FramesCollector,
        middlewares: List<FramesProcessorMiddleware> = emptyList(),
    ) = FramesProcessorService(
        framesCollector = collector,
        middlewares = middlewares,
        parallelProcessorWorks = 1,
        processingThrottlingMillis = 0,
        rejectOldParallelHandling = false,
        scope = scope,
    )

    /** Adapts [block] to a middleware instance. */
    private fun middleware(block: suspend (FrameData) -> FrameData) = object : FramesProcessorMiddleware {
        /** Processes [frame] by invoking the test's [block]. */
        override suspend fun process(frame: FrameData): FrameData = block(frame)
    }

    /** Creates a one-byte frame carrying [value]. */
    private fun frame(value: Byte) = ByteArrayFrameData(byteArrayOf(value), MetaContainer.EMPTY)

    /** Mutable frame collector used to publish source-map snapshots during tests. */
    private class TestFramesCollector(
        initialFlows: Map<FramesSourceId, Flow<FrameData>> = emptyMap(),
    ) : FramesCollector {
        /** Current source-to-flow snapshot. */
        private var flows = initialFlows

        /** Observable identifiers from the current snapshot. */
        override val sourcesListUpdatesFlow: MutableStateFlow<Set<FramesSourceId>> =
            MutableStateFlow(initialFlows.keys)

        /** Returns the flow currently associated with [id], if present. */
        override fun allocateFramesFlow(id: FramesSourceId): Flow<FrameData>? = flows[id]

        /** Replaces the source snapshot with [newFlows] and publishes its identifiers. */
        fun setFlows(newFlows: Map<FramesSourceId, Flow<FrameData>>) {
            flows = newFlows
            sourcesListUpdatesFlow.value = newFlows.keys
        }
    }
}

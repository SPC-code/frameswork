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

@OptIn(ExperimentalCoroutinesApi::class)
class FramesProcessorServiceTests {
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

    private fun middleware(block: suspend (FrameData) -> FrameData) = object : FramesProcessorMiddleware {
        override suspend fun process(frame: FrameData): FrameData = block(frame)
    }

    private fun frame(value: Byte) = ByteArrayFrameData(byteArrayOf(value), MetaContainer.EMPTY)

    private class TestFramesCollector(
        initialFlows: Map<FramesSourceId, Flow<FrameData>> = emptyMap(),
    ) : FramesCollector {
        private var flows = initialFlows
        override val sourcesListUpdatesFlow: MutableStateFlow<Set<FramesSourceId>> =
            MutableStateFlow(initialFlows.keys)

        override fun allocateFramesFlow(id: FramesSourceId): Flow<FrameData>? = flows[id]

        fun setFlows(newFlows: Map<FramesSourceId, Flow<FrameData>>) {
            flows = newFlows
            sourcesListUpdatesFlow.value = newFlows.keys
        }
    }
}

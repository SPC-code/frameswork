package space.kscience.frameswork.features.frames.common.services

import dev.inmo.micro_utils.meta.MetaContainer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import space.kscience.frameswork.features.frames.common.models.ByteArrayFrameData
import space.kscience.frameswork.features.frames.common.models.FrameData
import space.kscience.frameswork.features.frames.common.models.FramesSource
import space.kscience.frameswork.features.frames.common.models.FramesSourceId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultFramesCollectorTests {
    @Test
    fun sourcesAndAllocatedFlowsFollowAvailableSources() = runTest {
        val sourceId = FramesSourceId("camera-a")
        val source = TrackingFramesSource()
        val sourceCollector = TestFramesSourcesCollector()
        val collector = DefaultFramesCollector(sourceCollector, backgroundScope)
        runCurrent()

        assertEquals(emptySet(), collector.sourcesListUpdatesFlow.value)
        assertNull(collector.allocateFramesFlow(sourceId))

        sourceCollector.setSources(mapOf(sourceId to source))
        runCurrent()

        assertEquals(setOf(sourceId), collector.sourcesListUpdatesFlow.value)
        assertNotNull(collector.allocateFramesFlow(sourceId))

        sourceCollector.setSources(emptyMap())
        runCurrent()

        assertEquals(emptySet(), collector.sourcesListUpdatesFlow.value)
        assertNull(collector.allocateFramesFlow(sourceId))
    }

    @Test
    fun subscribersShareOneSourceCollectionAndItRestartsAfterLastSubscriberLeaves() = runTest {
        val sourceId = FramesSourceId("shared-camera")
        val source = TrackingFramesSource()
        val sourceCollector = TestFramesSourcesCollector(mapOf(sourceId to source))
        val collector = DefaultFramesCollector(sourceCollector, backgroundScope)
        runCurrent()
        val frames = assertNotNull(collector.allocateFramesFlow(sourceId))
        val firstReceived = mutableListOf<FrameData>()
        val secondReceived = mutableListOf<FrameData>()

        val firstSubscriber = backgroundScope.launch {
            frames.collect { firstReceived += it }
        }
        val secondSubscriber = backgroundScope.launch {
            frames.collect { secondReceived += it }
        }
        runCurrent()

        assertEquals(1, source.collectionCount)
        val firstFrame = ByteArrayFrameData(byteArrayOf(1), MetaContainer.EMPTY)
        source.frames.emit(firstFrame)
        runCurrent()
        assertSame(firstFrame, firstReceived.single())
        assertSame(firstFrame, secondReceived.single())

        firstSubscriber.cancelAndJoin()
        runCurrent()
        assertEquals(0, source.cancellationCount)

        secondSubscriber.cancelAndJoin()
        runCurrent()
        assertEquals(1, source.cancellationCount)

        val restartedSubscriber = backgroundScope.launch {
            frames.collect { }
        }
        runCurrent()
        assertEquals(2, source.collectionCount)

        restartedSubscriber.cancelAndJoin()
        runCurrent()
        assertEquals(2, source.cancellationCount)
    }

    @Test
    fun replacingConnectorSwitchesAnExistingAllocatedFlowToTheNewSource() = runTest {
        val sourceId = FramesSourceId("replaceable-camera")
        val originalSource = TrackingFramesSource()
        val replacementSource = TrackingFramesSource()
        val sourceCollector = TestFramesSourcesCollector(mapOf(sourceId to originalSource))
        val collector = DefaultFramesCollector(sourceCollector, backgroundScope)
        runCurrent()
        val frames = assertNotNull(collector.allocateFramesFlow(sourceId))
        val received = mutableListOf<FrameData>()
        val subscriber = backgroundScope.launch {
            frames.collect { received += it }
        }
        runCurrent()

        val originalFrame = ByteArrayFrameData(byteArrayOf(1), MetaContainer.EMPTY)
        originalSource.frames.emit(originalFrame)
        runCurrent()

        sourceCollector.setSources(mapOf(sourceId to replacementSource))
        runCurrent()
        val replacementFrame = ByteArrayFrameData(byteArrayOf(2), MetaContainer.EMPTY)
        replacementSource.frames.emit(replacementFrame)
        runCurrent()

        assertEquals(1, originalSource.cancellationCount)
        assertEquals(1, replacementSource.collectionCount)
        assertEquals(listOf<FrameData>(originalFrame, replacementFrame), received)

        subscriber.cancelAndJoin()
    }

    private class TestFramesSourcesCollector(
        initialSources: Map<FramesSourceId, FramesSource> = emptyMap(),
    ) : FramesSourcesCollector {
        private var activeSources = initialSources
        private val connectorFlows = initialSources
            .mapValuesTo(mutableMapOf()) { MutableStateFlow<FramesSource?>(it.value) }
        private val sourceIds = MutableStateFlow(initialSources.keys)

        override val framesSourcesIdsListUpdatesFlow: StateFlow<Set<FramesSourceId>> = sourceIds

        override suspend fun getAvailableFramesSourcesIds(): Set<FramesSourceId> = activeSources.keys

        override fun allocateConnectorFlow(id: FramesSourceId): StateFlow<FramesSource?> =
            connectorFlows.getOrPut(id) { MutableStateFlow(null) }

        fun setSources(sources: Map<FramesSourceId, FramesSource>) {
            activeSources = sources
            (connectorFlows.keys + sources.keys).forEach { id ->
                connectorFlows.getOrPut(id) { MutableStateFlow(null) }.value = sources[id]
            }
            sourceIds.value = sources.keys
        }
    }

    private class TrackingFramesSource : FramesSource {
        val frames = MutableSharedFlow<FrameData>()
        var collectionCount = 0
            private set
        var cancellationCount = 0
            private set

        override fun allocateFlow(): Flow<FrameData> = flow {
            collectionCount++
            try {
                frames.collect { emit(it) }
            } finally {
                cancellationCount++
            }
        }
    }
}

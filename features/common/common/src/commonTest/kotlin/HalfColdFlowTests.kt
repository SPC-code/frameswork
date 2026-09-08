package space.kscience.frameswork.features.common.common.utils

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class HalfColdFlowTests {
    @Test
    fun subscriberBeforeInnerFlowReceivesValuesOnceFlowArrives() = runTest {
        val innerFlow = MutableSharedFlow<Int>(replay = 1)
        val outerFlow = MutableSharedFlow<Flow<Int>>(replay = 1)

        val scope = backgroundScope + Dispatchers.Default
        val halfCold = outerFlow.toHalfColdFlow(scope)

        // Subscribe before any inner flow is emitted
        val received = MutableRedeliverStateFlow<List<Int>>(emptyList())
        val job = halfCold.first.subscribeLoggingDropExceptions(scope) {
            received.value += it
        }
        advanceUntilIdle()

        outerFlow.emit(innerFlow)
        advanceUntilIdle()

        innerFlow.emit(42)
        advanceUntilIdle()

        assertEquals(listOf(42), received.first { it.isNotEmpty() })
        job.cancel()
    }

    @Test
    fun innerFlowIsCollectedOnlyWhileThereAreSubscribers() = runTest {
        var collectionCount = 0
        var cancellationCount = 0
        val innerFlow = flow<Int> {
            collectionCount++
            try {
                awaitCancellation()
            } finally {
                cancellationCount++
            }
        }
        val outerFlow = MutableSharedFlow<Flow<Int>>(replay = 1).apply {
            tryEmit(innerFlow)
        }
        val (halfCold, _) = outerFlow.toHalfColdFlow(backgroundScope)
        runCurrent()

        assertEquals(0, collectionCount)

        val firstSubscriber = backgroundScope.launch {
            halfCold.collect { }
        }
        runCurrent()
        assertEquals(1, collectionCount)

        val secondSubscriber = backgroundScope.launch {
            halfCold.collect { }
        }
        runCurrent()
        assertEquals(1, collectionCount)

        firstSubscriber.cancelAndJoin()
        runCurrent()
        assertEquals(0, cancellationCount)

        secondSubscriber.cancelAndJoin()
        runCurrent()
        assertEquals(1, cancellationCount)

        val restartedSubscriber = backgroundScope.launch {
            halfCold.collect { }
        }
        runCurrent()
        assertEquals(2, collectionCount)

        restartedSubscriber.cancelAndJoin()
        runCurrent()
        assertEquals(2, cancellationCount)
    }

    @Test
    fun activeSubscribersReceiveEachValueFromOneSharedCollection() = runTest {
        val innerFlow = MutableSharedFlow<Int>()
        val outerFlow = MutableSharedFlow<Flow<Int>>(replay = 1).apply {
            tryEmit(innerFlow)
        }
        val (halfCold, _) = outerFlow.toHalfColdFlow(backgroundScope)
        val firstReceived = mutableListOf<Int>()
        val secondReceived = mutableListOf<Int>()
        val firstSubscriber = backgroundScope.launch {
            halfCold.collect { firstReceived += it }
        }
        val secondSubscriber = backgroundScope.launch {
            halfCold.collect { secondReceived += it }
        }
        runCurrent()

        innerFlow.emit(1)
        innerFlow.emit(2)
        runCurrent()

        assertEquals(listOf(1, 2), firstReceived)
        assertEquals(listOf(1, 2), secondReceived)
        firstSubscriber.cancel()
        secondSubscriber.cancel()
    }
}

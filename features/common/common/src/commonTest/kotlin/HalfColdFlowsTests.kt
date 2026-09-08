package space.kscience.frameswork.features.common.common.utils

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.plus
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

@OptIn(ExperimentalCoroutinesApi::class)
class HalfColdFlowsTests {
    @Test
    fun subscriberReceivesValuesFromKeyedFlow() = runTest {
        val innerFlow = MutableSharedFlow<Int>(replay = 1)
        val sourceFlow = MutableSharedFlow<Map<String, Flow<Int>>>(replay = 1)

        val scope = backgroundScope + Dispatchers.Default
        val (resultState, _) = sourceFlow.halfColdFlows(scope)

        sourceFlow.emit(mapOf("a" to innerFlow))
        advanceUntilIdle()

        val received = MutableRedeliverStateFlow<List<Int>>(emptyList())
        val keyedFlow = resultState.first { it.containsKey("a") }.getValue("a")
        val job = keyedFlow.subscribeLoggingDropExceptions(scope) {
            received.value += it
        }
        advanceUntilIdle()

        innerFlow.emit(42)
        advanceUntilIdle()

        assertEquals(listOf(42), received.first { it.isNotEmpty() })
        job.cancel()
    }

    @Test
    fun resultKeysFollowSourceMapAndRemovedFlowIsReusedWhenStillReferenced() = runTest {
        val sourceFlow = MutableStateFlow<Map<String, Flow<Int>>>(emptyMap())
        val (resultState, _) = sourceFlow.halfColdFlows(backgroundScope)
        runCurrent()

        sourceFlow.value = mapOf("a" to emptyFlow())
        runCurrent()
        val originalFlow = resultState.value.getValue("a")
        assertEquals(setOf("a"), resultState.value.keys)

        sourceFlow.value = emptyMap()
        runCurrent()
        assertEquals(emptySet(), resultState.value.keys)

        sourceFlow.value = mapOf("a" to emptyFlow(), "b" to emptyFlow())
        runCurrent()
        assertEquals(setOf("a", "b"), resultState.value.keys)
        assertSame(originalFlow, resultState.value.getValue("a"))
    }

    @Test
    fun onlySubscribedKeyCollectsItsSourceFlow() = runTest {
        var firstCollections = 0
        var firstCancellations = 0
        var secondCollections = 0
        val firstValues = MutableSharedFlow<Int>()
        val firstFlow = flow {
            firstCollections++
            try {
                firstValues.collect { emit(it) }
            } finally {
                firstCancellations++
            }
        }
        val secondFlow = flow<Int> {
            secondCollections++
            awaitCancellation()
        }
        val sourceFlow = MutableStateFlow(
            mapOf<String, Flow<Int>>(
                "first" to firstFlow,
                "second" to secondFlow,
            ),
        )
        val (resultState, _) = sourceFlow.halfColdFlows(backgroundScope)
        runCurrent()
        val received = mutableListOf<Int>()

        val subscriber = backgroundScope.launch {
            resultState.value.getValue("first").collect { received += it }
        }
        runCurrent()

        assertEquals(1, firstCollections)
        assertEquals(0, secondCollections)
        firstValues.emit(9)
        runCurrent()
        assertEquals(listOf(9), received)

        subscriber.cancelAndJoin()
        runCurrent()
        assertEquals(1, firstCancellations)
    }
}

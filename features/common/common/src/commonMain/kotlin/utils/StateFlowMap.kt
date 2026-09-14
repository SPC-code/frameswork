package space.kscience.frameswork.features.common.common.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform

/**
 * A read-only [StateFlow] view that transforms values from [input] on access and collection.
 *
 * The view does not launch a coroutine or cache transformed values. Reading [value] and [replayCache]
 * applies [transform] to the corresponding values from [input].
 *
 * @param I Type emitted by the source state flow.
 * @param R Type exposed by the mapped state flow.
 * @param input Source state flow.
 * @property transform Synchronous mapping applied to source values.
 */
class StateFlowMap<I, R>(private val input: StateFlow<I>, val transform: (I) -> R) : StateFlow<R> {
    /** The current source value after applying [transform]. */
    override val value: R
        get() = input.value.let(transform)

    /** The source replay cache after applying [transform] to each value. */
    override val replayCache: List<R>
        get() = input.replayCache.map(transform)

    /**
     * Collects [input] and emits each value after applying [transform].
     *
     * @param collector Collector receiving transformed values.
     */
    override suspend fun collect(collector: FlowCollector<R>): Nothing {
        input.map(transform).collect(collector)
        error("StateFlowMap accidentally completed")
    }
}

/**
 * Creates a mapped, read-only [StateFlow] view without starting an additional coroutine.
 *
 * @receiver Source state flow.
 * @param transform Synchronous mapping applied on value access and collection.
 * @return A [StateFlowMap] backed by this state flow.
 */
fun <I, R> StateFlow<I>.mapAsStateFlow(transform: (I) -> R): StateFlow<R> {
    return StateFlowMap(this, transform)
}

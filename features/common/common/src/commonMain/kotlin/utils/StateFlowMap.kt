package space.kscience.frameswork.features.common.common.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform

class StateFlowMap<I, R>(private val input: StateFlow<I>, val transform: (I) -> R) : StateFlow<R> {
    override val value: R
        get() = input.value.let(transform)
    override val replayCache: List<R>
        get() = input.replayCache.map(transform)

    override suspend fun collect(collector: FlowCollector<R>): Nothing {
        input.map(transform).collect(collector)
        error("StateFlowMap accidentally completed")
    }
}

fun <I, R> StateFlow<I>.mapAsStateFlow(transform: (I) -> R): StateFlow<R> {
    return StateFlowMap(this, transform)
}

package space.kscience.frameswork.features.common.common.utils

import dev.inmo.micro_utils.coroutines.LinkedSupervisorScope
import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.job


/**
 * Converts keyed source flows into a [StateFlow] of subscription-aware output flows.
 *
 * The result map follows the keys of each source map. Every output flow uses [toHalfColdFlow], so its
 * source is collected only while that particular output has subscribers. Replacing the source flow for
 * an existing key is observed by that keyed adapter. When a key disappears, its output is held through a
 * [WeakRef]; if the key returns before reclamation, the same output instance is reused.
 *
 * @param K Type used to identify source flows.
 * @param V Value type emitted by the source flows.
 * @param scope Parent scope for source-map observation and all keyed adapters.
 * @return The state flow of keyed output flows and the [Job] that owns the linked supervisor scope.
 *   Cancelling the job stops map observation and all child work.
 */

fun <K, V> Flow<Map<K, Flow<V>>>.halfColdFlows(
    scope: CoroutineScope,
): Pair<StateFlow<Map<K, Flow<V>>>, Job> {
    val flowsStateFlow = MutableRedeliverStateFlow<Map<K, Flow<V>>>(emptyMap())
    val weakFlowsStateFlow = MutableRedeliverStateFlow<Map<K, WeakRef<Flow<V>>>>(emptyMap())

    val subscope = scope.LinkedSupervisorScope()

    subscribeLoggingDropExceptions(subscope) {
        val flowsStateFlowValue = flowsStateFlow.value
        val weakFlowsStateFlowValue = weakFlowsStateFlow.value
        val keysNeedToAdd = (it.keys - flowsStateFlowValue.keys)
        val keysNeedToRemove = (flowsStateFlowValue.keys - it.keys)

        val flowsToAddInStrong = keysNeedToAdd.map { k ->
            val potentialWeakRef = weakFlowsStateFlowValue[k]
            val potentialFlow = potentialWeakRef ?.get()
            val outputFlow = potentialFlow ?:
                map {
                    it[k] ?: emptyFlow()
                }
                .toHalfColdFlow(subscope)
                .first
            k to outputFlow
        }

        val flowsToAddInWeak = keysNeedToRemove.map { k ->
            val existsFlow = flowsStateFlowValue.getValue(k)
            k to WeakRef(existsFlow)
        }

        flowsStateFlow.value = flowsStateFlow.value + flowsToAddInStrong - keysNeedToRemove
        weakFlowsStateFlow.value = weakFlowsStateFlow.value + flowsToAddInWeak - keysNeedToAdd
    }

    return flowsStateFlow to subscope.coroutineContext.job
}

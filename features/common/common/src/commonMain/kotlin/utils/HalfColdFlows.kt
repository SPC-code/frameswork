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
 * Transforms a `Flow` of `Map` objects containing `Flow`s into a `StateFlow` of `Map` objects with "half-cold" semantics,
 * ensuring that only currently subscribed `Flow`s are actively emitting values. This helps reduce unnecessary computation
 * and overhead in scenarios where not all `Flow`s in the map are actively needed by subscribers.
 *
 * ## Half-Cold Flow Behavior
 *
 * The function implements a "half-cold" pattern where flows:
 * - Become "hot" (actively collecting from source and emitting) when they have at least one subscriber
 * - Become "cold" (stop collecting from source) when subscriber count drops to zero
 * - Are automatically managed based on their presence in the source flow map
 *
 * ## Internal Mechanism
 *
 * For each key `K` in the incoming map:
 * - A dedicated `Job` is created to manage the flow's lifecycle
 * - A `MutableSharedFlow<V>` is created as the output flow that subscribers will collect from
 * - The job monitors both the subscription count and changes to the source flow for that key
 * - When subscribers exist, values are transferred from the source flow to the output flow
 * - When the key is removed from the source map, the output flow is removed from the result state
 *
 * ## WeakRef Usage and Flow Resurrection
 *
 * @note This function uses `WeakRef` (weak references) to implement a flow resurrection mechanism. When a flow
 * for a specific key is removed from the source map, instead of immediately destroying the `MutableSharedFlow`,
 * it is stored in the `predeadFlows` map using a `WeakRef`. This allows the flow instance to be garbage collected
 * if no strong references exist, but if the same key reappears in the source map shortly after removal (before GC),
 * the original flow instance can be resurrected and reused. This prevents unnecessary flow recreation and maintains
 * continuity for subscribers that might still hold references to the flow, avoiding potential subscription breaks
 * and memory leaks from orphaned flow instances.
 *
 * @note The `predeadFlows` map serves as a temporary cache of recently removed flows. When a key is removed from
 * the source map, its corresponding output flow is placed in `predeadFlows` with a weak reference. If the key
 * reappears before the flow is garbage collected, the same flow instance is restored to `resultStatesFlows`,
 * maintaining subscriber connections. If the flow has been garbage collected, a new `MutableSharedFlow` is created
 * instead. This mechanism balances memory efficiency (allowing GC when needed) with performance optimization
 * (reusing instances when possible).
 *
 * @param K The type of keys used in the map to identify different flows
 * @param V The type of values emitted by the flows in the map
 * @param scope The `CoroutineScope` in which any internal `Job`s will be launched and executed. This scope ensures
 * proper lifecycle management of resources associated with the created `StateFlow`. All internal operations are
 * executed within a `LinkedSupervisorScope` derived from this scope.
 * @return A `StateFlow` representing a `Map` with keys of type `K` and values of type `Flow<V>`, where each `Flow`
 * is only "hot" (actively emitting) if it has subscribers, and "cold" (not collecting from source) otherwise.
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

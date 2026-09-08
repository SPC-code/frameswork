package space.kscience.frameswork.features.common.common.utils

import dev.inmo.micro_utils.coroutines.LinkedSupervisorScope
import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.micro_utils.coroutines.suspendPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Converts a `Flow<Flow<T>>` into a single "half-cold" [SharedFlow] that collects from the latest
 * inner [Flow] only while there are active subscribers.
 *
 * ## Half-cold semantics
 *
 * Unlike a fully hot flow (always collecting) or a fully cold flow (restarts for every subscriber),
 * a half-cold flow has the following lifecycle:
 * - **No subscribers:** the active transfer job is cancelled and the strong reference to the output
 *   [MutableSharedFlow] is dropped; only a [WeakRef] is kept.
 * - **First subscriber arrives:** if the weak reference is still alive the same [MutableSharedFlow]
 *   instance is reused and a new transfer job is started to pipe values from the latest inner flow into the output flow.
 * - **All subscribers leave again:** the cycle repeats — transfer is stopped, strong reference is
 *   released.
 *
 * The internal [CoroutineScope] (`subscope`) is cancelled permanently when both the strong *and*
 * the weak reference to the output flow have been cleared (i.e. the GC has reclaimed the object
 * and no subscribers hold it alive). At that point [suspendPoint] causes the coroutine to yield
 * so the cancellation takes effect.
 *
 * ## Thread safety
 *
 * All state mutations ([existsTransferJob], [tmpOutputSharedFlow], [predeadFlow]) are synchronized
 * through a [Mutex] inside `updateState`, making the function safe for concurrent calls from the
 * subscriptions-count observer and the inner-flow observer.
 *
 * @param T Element type of the inner flows.
 * @param scope Parent [CoroutineScope] for the internal supervisor scope. The returned flow stays
 *   alive at most as long as [scope] stays active.
 * @return A read-only [SharedFlow] that emits values from the most recently received inner [Flow]
 *   whenever at least one subscriber is collecting it.
 */
fun <T> Flow<Flow<T>>.toHalfColdFlow(
    scope: CoroutineScope,
    replay: Int = 0,
    onBufferOverflow: BufferOverflow = BufferOverflow.DROP_OLDEST,
): Pair<SharedFlow<T>, Job> {
    var outputSharedFlow: MutableSharedFlow<T>? = MutableSharedFlow<T>(
        replay = replay,
        extraBufferCapacity = 1,
        onBufferOverflow = onBufferOverflow
    )
    val outputReadonlySharedFlow = outputSharedFlow!!.asSharedFlow()
    val subscriptionsCountFlow: StateFlow<Int> = outputSharedFlow.subscriptionCount
    var predeadFlow: WeakRef<MutableSharedFlow<T>>? = null

    val subscope = scope.LinkedSupervisorScope()
    val sourceFlow = this

    val sourceCompletionJob = Job(subscope.coroutineContext.job)
    val latestReceivedFlow = MutableRedeliverStateFlow<Flow<T>?>(null)


    var existsTransferJob: Job? = null

    val updateLock = Mutex()

    suspend fun updateState() {
        val inputFlow = latestReceivedFlow.value
        val subscriptionsCount = subscriptionsCountFlow.value
        val capturedOutputFlow = outputSharedFlow
        val predeadFlowValue = predeadFlow ?.get()

        if (capturedOutputFlow == null && predeadFlowValue == null) {
            subscope.cancel()
            suspendPoint()
            return
        }

        when {
            subscriptionsCount == 0 -> {
                existsTransferJob ?.cancel()
                existsTransferJob = null
                outputSharedFlow = null
                predeadFlow = capturedOutputFlow ?.let(::WeakRef) ?: predeadFlow
            }
            existsTransferJob == null || outputSharedFlow == null -> {
                val outputFlow = (capturedOutputFlow ?: predeadFlowValue) ?: error("Unexpected state: both flows are null")
                outputSharedFlow = outputFlow

                existsTransferJob ?.cancel()
                existsTransferJob = inputFlow ?.let {
                    subscope.launch {
                        inputFlow.collect(outputFlow)
                        if (sourceCompletionJob.isActive == false) {
                            // Both source flow and current data flow ended, nothing to collect left
                            subscope.cancel()
                        }
                    }
                }
            }
        }
    }
    subscriptionsCountFlow.subscribeLoggingDropExceptions(subscope) {
        updateLock.withLock {
            updateState()
        }
    }
    latestReceivedFlow.subscribeLoggingDropExceptions(subscope) {
        updateLock.withLock {
            updateState()
        }
    }

    subscope.launch(
        start = CoroutineStart.UNDISPATCHED
    ) {
        sourceFlow
            .onCompletion {
                sourceCompletionJob.complete()
            }
            .collect {
                latestReceivedFlow.value = it
            }
    }

    return outputReadonlySharedFlow to subscope.coroutineContext.job
}

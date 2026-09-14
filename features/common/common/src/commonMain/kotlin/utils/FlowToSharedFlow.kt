package space.kscience.frameswork.features.common.common.utils

import dev.inmo.micro_utils.coroutines.LinkedSupervisorScope
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
 * Exposes this flow as a subscription-aware [SharedFlow].
 *
 * The source is collected once while the returned flow has subscribers. Source collection is cancelled
 * after the final subscriber leaves and is restarted if a subscriber arrives later. Internal work runs in
 * a linked supervisor scope derived from [scope].
 *
 * @param scope Parent scope for collection and subscription tracking.
 * @param replay Number of recent values replayed to a new subscriber.
 * @param onBufferOverflow Strategy used when the shared flow's replay and extra buffer are full.
 * @return The read-only shared flow and the [Job] that owns its internal linked supervisor scope.
 *   Cancelling the job stops all internal work.
 * @throws IllegalArgumentException If [replay] is negative.
 */
fun <T> Flow<T>.toSharedFlow(
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


    var existsTransferJob: Job? = null

    val updateLock = Mutex()

    suspend fun updateState() {
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
                existsTransferJob = sourceFlow.let {
                    subscope.launch {
                        sourceFlow.collect(outputFlow)
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

    subscope.launch(
        start = CoroutineStart.UNDISPATCHED
    ) {
        sourceFlow
            .onCompletion {
                sourceCompletionJob.complete()
            }
    }

    return outputReadonlySharedFlow to subscope.coroutineContext.job
}

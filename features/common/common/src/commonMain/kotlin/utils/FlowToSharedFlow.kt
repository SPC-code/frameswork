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
 * Transforms the current [Flow] into a [SharedFlow], providing a mechanism to reuse the emitted values
 * across multiple subscribers while managing subscriptions dynamically.
 *
 * The method sets up a [SharedFlow] with configurable replay behavior and buffering, while also managing
 * lifecycle and resource cleanup using a [CoroutineScope].
 *
 * @param scope The [CoroutineScope] in which collection from the source [Flow] and emissions to the [SharedFlow] will occur.
 * @param replay The number of values that will be replayed to new subscribers. Defaults to `0`.
 * @param onBufferOverflow The strategy to handle buffer overflows in the [SharedFlow]. Default is [BufferOverflow.DROP_OLDEST].
 * @return A [Pair] where:
 *   - The first component is the [SharedFlow] that emits values from the source [Flow].
 *   - The second component is the [Job] representing the underlying coroutine scope created for managing the collection and emissions.
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

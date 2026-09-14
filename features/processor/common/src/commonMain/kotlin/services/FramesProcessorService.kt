package space.kscience.frameswork.features.processor.common.services

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning
import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.SmartMutex
import dev.inmo.micro_utils.coroutines.runCatchingLogging
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import korlibs.time.DateTime
import korlibs.time.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import space.kscience.frameswork.features.frames.common.models.FramesSourceId
import space.kscience.frameswork.features.frames.common.models.FrameData
import space.kscience.frameswork.features.frames.common.services.FramesCollector
import space.kscience.frameswork.features.common.common.utils.halfColdFlows
import space.kscience.frameswork.features.frames.common.models.FrameReceiveTimestamp

/**
 * Applies an ordered middleware pipeline to frames obtained from a [FramesCollector].
 *
 * Source changes are observed continuously. The per-source output flows are shared and collect their upstream
 * flow only while subscribed. Middleware failures are logged and skipped, leaving the last successful frame as
 * the input to the next middleware.
 *
 * @param framesCollector Upstream collector that supplies source identifiers and frame flows.
 * @param middlewares Middleware stages, applied in list order for every processed frame.
 * @param parallelProcessorWorks Maximum number of internally scheduled source-flow processing jobs allowed at
 *   once; must be positive. Direct [process] calls are not governed by this limit.
 * @param processingThrottlingMillis Interval for latest-frame coalescing, or `null` for the immediate
 *   best-effort path, which does not emit scheduled results through the allocated source flows.
 * @param rejectOldParallelHandling Whether work with an older [FrameReceiveTimestamp] should be cancelled after
 *   a newer result is observed.
 * @param scope Scope that owns source observation and shared-flow jobs.
 * @throws IllegalArgumentException If [parallelProcessorWorks] is not positive.
 */
class FramesProcessorService(
    private val framesCollector: FramesCollector,
    private val middlewares: List<FramesProcessorMiddleware>,
    private val parallelProcessorWorks: Int,
    private val processingThrottlingMillis: Int?,
    private val rejectOldParallelHandling: Boolean,
    private val scope: CoroutineScope
) : FramesProcessor {
    /** Logger used for recoverable middleware failures. */
    private val Log = KSLog("CameraFramesProcessorService")

    /** Limits concurrently running frame-processing jobs. */
    private val asyncsSemaphore = Semaphore(parallelProcessorWorks)

    /** Configured throttling interval as a duration, or `null` when throttling is disabled. */
    private val processingThrottlingDuration = processingThrottlingMillis ?.milliseconds

    /**
     * Starts processing [frame] when a concurrency permit is immediately available.
     *
     * Processing failures fall back to the original frame. [onSend] receives the resulting frame before the
     * permit is released.
     *
     * @param frame Frame to process asynchronously in this producer scope.
     * @param onSend Callback invoked with the processed or fallback frame.
     * @return The launched job, or `null` when all permits are occupied.
     */
    private fun ProducerScope<FrameData>.tryLaunchFrameHandling(frame: FrameData, onSend: suspend (FrameData) -> Unit = {}): Job? {
        return if (asyncsSemaphore.tryAcquire()) {
            launch {
                val result = runCatchingLogging {
                    process(frame)
                }.getOrElse { e ->
                    frame
                }
                onSend(result)
            }.apply {
                invokeOnCompletion {
                    asyncsSemaphore.release()
                }
            }
        } else {
            null
        }
    }

    /**
     * Collects [baseFlow], schedules frame processing, and closes when the upstream source set changes.
     *
     * A non-null throttling interval keeps only the latest pending frame between scheduling opportunities.
     *
     * @param baseFlow Current upstream flow for one source.
     */
    private suspend fun ProducerScope<FrameData>.doMappingFlowJob(baseFlow: Flow<FrameData>) {
        val throttlingMutex = processingThrottlingMillis ?.let { SmartMutex.Mutable() }
        framesCollector
            .sourcesListUpdatesFlow
            .drop(1) // drop current state
            .take(1) // taking one next state
            .subscribeLoggingDropExceptions(this) { // subscribe in channel flow scope
                this.cancel() // Closing of channel flow when new update came
            }
        val latestResultDateTime = MutableRedeliverStateFlow<DateTime>(DateTime(0))
        val frameHandler: suspend (FrameData) -> Unit = when {
            processingThrottlingDuration != null -> {
                val dataFlow = MutableRedeliverStateFlow<FrameData?>(null)
                val dataMutex = Mutex()
                launch {
                    while (isActive) {
                        val currentData = dataMutex.withLock {
                            val value = dataFlow.value
                            dataFlow.value = null
                            value
                        }
                        if (currentData != null) {
                            val handlingJob = tryLaunchFrameHandling(
                                currentData
                            ) {
                                send(it)
                                latestResultDateTime.value = it.meta[FrameReceiveTimestamp] ?: return@tryLaunchFrameHandling
                            }
                            if (handlingJob != null) {
                                val currentTimestampDateTime = currentData.meta[FrameReceiveTimestamp]
                                if (rejectOldParallelHandling && currentTimestampDateTime != null) {
                                    latestResultDateTime
                                        .takeWhile {
                                            if (it > currentTimestampDateTime) {
                                                handlingJob.cancel()
                                                false
                                            } else {
                                                handlingJob.isActive
                                            }
                                        }
                                        .launchIn(this)
                                }
                                delay(processingThrottlingDuration)
                            }
                        }
                        dataFlow.filterNotNull().first()
                    }
                }
                suspend { it: FrameData ->
                    dataMutex.withLock {
                        dataFlow.value = it
                    }
                }
            }
            else -> {
                {
                    if (throttlingMutex ?.tryLock() != false && asyncsSemaphore.tryAcquire()) {
                        val handlingJob = tryLaunchFrameHandling(it)
                        if (handlingJob != null) {
                            val currentTimestampDateTime = it.meta[FrameReceiveTimestamp]
                            if (rejectOldParallelHandling && currentTimestampDateTime != null) {
                                latestResultDateTime
                                    .takeWhile {
                                        if (it > currentTimestampDateTime) {
                                            handlingJob.cancel()
                                            false
                                        } else {
                                            handlingJob.isActive
                                        }
                                    }
                                    .launchIn(this)
                            }
                        }
                    }
                }
            }
        }
        processingThrottlingDuration ?.let {
            throttlingMutex ?.let {
                launch {
                    throttlingMutex.lockStateFlow.filter { it }.collect {
                        delay(processingThrottlingDuration)
                        runCatchingLogging {
                            throttlingMutex.unlock()
                        }
                    }
                }
            }
        }
        baseFlow.collect {
            frameHandler(it)
        }
        this@doMappingFlowJob.cancel()
    }

    /** Latest map of source identifiers to their processed channel flows. */
    private val flows: Flow<Map<FramesSourceId, Flow<FrameData>>> = framesCollector // TODO:: rewrite with shared flows to support preprocessing in one common flow
        .sourcesListUpdatesFlow
        .mapLatest {
            it.mapNotNull {
                val baseFlow = framesCollector
                    .allocateFramesFlow(it)
                    ?: return@mapNotNull null
                it to channelFlow<FrameData> {
                    doMappingFlowJob(baseFlow)
                }
            }.toMap()
        }

    /** Identifiers whose upstream collectors currently return an allocatable flow. */
    override val sourcesListUpdatesFlow: StateFlow<Set<FramesSourceId>> = flows.map {
        it.keys
    }.stateIn(scope, SharingStarted.Eagerly, emptySet())

    /** Subscription-aware source-flow map together with its lifecycle job. */
    private val persistentFlowsAndJob = flows.halfColdFlows(scope)

    /** Current subscription-aware processed flow for each available source. */
    private val persistentFlows = persistentFlowsAndJob.first

    /** Lifecycle job retained for the subscription-aware source-flow map. */
    private val persistentFlowsJob = persistentFlowsAndJob.second

    /**
     * Applies every configured middleware in order.
     *
     * When a middleware throws, the failure is logged and the next middleware receives the last successfully
     * produced frame.
     *
     * @param frame Frame supplied to the first middleware.
     * @return The result of the last successful middleware, or [frame] when none changes it.
     */
    override suspend fun process(frame: FrameData): FrameData {
        var current = frame
        middlewares.forEach {
            runCatching {
                current = it.process(current)
            }
                .onFailure { e ->
                    Log.warning(e) {
                        "Processor middleware $it processed frame with error"
                    }
                }
        }

        return current
    }

    /**
     * Returns the currently allocated processed flow for [id].
     *
     * @param id Source identifier to look up in the current source snapshot.
     * @return The shared processed flow, or `null` while the source is unavailable.
     */
    override fun allocateFramesFlow(
        id: FramesSourceId,
    ): Flow<FrameData>? {
        return persistentFlows.value[id]
    }

    /**
     * Returns a flow that follows [id] across source removal, addition, and replacement.
     *
     * @param id Source identifier to follow.
     * @return A flow that emits from the current processed source and remains quiet while it is absent.
     */
    override fun allocatePersistentFramesFlow(id: FramesSourceId): Flow<FrameData> {
        return persistentFlows.flatMapLatest {
            it[id] ?: emptyFlow()
        }
    }
}

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

class FramesProcessorService(
    private val framesCollector: FramesCollector,
    private val middlewares: List<FramesProcessorMiddleware>,
    private val parallelProcessorWorks: Int,
    private val processingThrottlingMillis: Int?,
    private val rejectOldParallelHandling: Boolean,
    private val scope: CoroutineScope
) : FramesProcessor {
    private val Log = KSLog("CameraFramesProcessorService")
    private val asyncsSemaphore = Semaphore(parallelProcessorWorks)
    private val processingThrottlingDuration = processingThrottlingMillis ?.milliseconds

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

    override val sourcesListUpdatesFlow: StateFlow<Set<FramesSourceId>> = flows.map {
        it.keys
    }.stateIn(scope, SharingStarted.Eagerly, emptySet())

    private val persistentFlowsAndJob = flows.halfColdFlows(scope)
    private val persistentFlows = persistentFlowsAndJob.first
    private val persistentFlowsJob = persistentFlowsAndJob.second

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

    override fun allocateFramesFlow(
        id: FramesSourceId,
    ): Flow<FrameData>? {
        return persistentFlows.value[id]
    }

    override fun allocatePersistentFramesFlow(id: FramesSourceId): Flow<FrameData> {
        return persistentFlows.flatMapLatest {
            it[id] ?: emptyFlow()
        }
    }
}

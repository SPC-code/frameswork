package space.kscience.frameswork.features.processor.server

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.firstNotNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import space.kscience.frameswork.features.frames.common.services.FramesCollector
import space.kscience.frameswork.features.processor.common.services.FramesProcessor
import space.kscience.frameswork.features.processor.common.services.FramesProcessorMiddleware
import space.kscience.frameswork.features.processor.common.services.FramesProcessorService

/**
 * Builds and exposes the frame processors described by [processorsConfigs].
 *
 * Every map entry produces a [FramesProcessorService]. An identifier in
 * [ProcessorConfig.middlewares] is resolved as a [FramesProcessorMiddleware.Factory] identifier
 * first and as another configured processor identifier otherwise. Middleware instances created
 * while resolving those identifiers are shared through a container-wide cache.
 *
 * Processor initialization is launched in [scope]. [availableProcessors] and [getProcessor]
 * suspend until that initialization publishes the processor map.
 *
 * @param framesCollector source of the frame flows processed by every configured processor.
 * @param scope coroutine scope used to initialize processors and run their services.
 * @param processorsConfigs processor definitions keyed by their externally visible identifiers.
 * @param middlewaresFactories factories available for entries in each middleware chain.
 */
class ProcessorsContainer(
    framesCollector: FramesCollector,
    scope: CoroutineScope,
    private val processorsConfigs: Map<String, ProcessorConfig>,
    middlewaresFactories: List<FramesProcessorMiddleware.Factory>
) {
    /**
     * Configuration of one named frame processor.
     *
     * @property middlewares ordered factory or processor identifiers applied to every frame.
     * @property parallelProcessing positive maximum number of frame-processing jobs allowed
     * concurrently.
     * @property processingThrottlingMillis delay between accepted processing jobs, or `null` to
     * disable time-based throttling.
     * @property rejectOldParallelHandling whether work for an older timestamped frame may be
     * cancelled after a newer result is produced.
     */
    @Serializable
    data class ProcessorConfig(
        val middlewares: List<String>,
        val parallelProcessing: Int = 8,
        val processingThrottlingMillis: Int? = 30,
        val rejectOldParallelHandling: Boolean = false
    )

    /** Processor map published after asynchronous initialization completes. */
    private val processors = MutableRedeliverStateFlow<Map<String, FramesProcessor>?>(null)

    init {
        val associatedFactories = middlewaresFactories.associateBy { it.id.string }
        val middlewaresCache = mutableMapOf<String, FramesProcessorMiddleware>()

        lateinit var gettersToFillMap: Map<String, List<suspend () -> FramesProcessorMiddleware>>
        val framesCollector: FramesCollector = framesCollector
        val scope: CoroutineScope = scope
        suspend fun getProcessor(processorId: String): FramesProcessor {
            val middlewaresGetters = gettersToFillMap[processorId] ?: error("Unable to find processor for id $processorId")

            return FramesProcessorService(
                framesCollector = framesCollector,
                middlewares = middlewaresGetters.map { it() },
                parallelProcessorWorks = processorsConfigs[processorId] ?.parallelProcessing ?: 1,
                processingThrottlingMillis = processorsConfigs[processorId] ?.processingThrottlingMillis,
                rejectOldParallelHandling = processorsConfigs[processorId] ?.rejectOldParallelHandling ?: false,
                scope = scope
            )
        }
        gettersToFillMap = processorsConfigs.map { (processorId, config) ->
            val middlewaresIds = config.middlewares
            processorId to middlewaresIds.map {
                suspend {
                    val middleware: FramesProcessorMiddleware = middlewaresCache[it] ?: associatedFactories[it] ?.createMiddleware() ?: getProcessor(it)
                    middlewaresCache[it] = middleware
                    middleware
                }
            }
        }.toMap()

        // Filling processors
        scope.launch {
            processors.value = gettersToFillMap.keys.associateWith {
                getProcessor(it)
            }
        }
    }

    /**
     * Returns the identifiers of all configured processors.
     *
     * @return configured processor identifiers after initialization completes.
     */
    suspend fun availableProcessors(): Set<String> = processors.firstNotNull().keys

    /**
     * Looks up a configured processor by [processorId].
     *
     * @param processorId identifier from the processor configuration map.
     * @return the initialized processor, or `null` when no processor has that identifier.
     */
    suspend fun getProcessor(processorId: String): FramesProcessor? = processors.firstNotNull().get(processorId)
}

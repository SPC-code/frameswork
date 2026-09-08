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

class ProcessorsContainer(
    framesCollector: FramesCollector,
    scope: CoroutineScope,
    private val processorsConfigs: Map<String, ProcessorConfig>,
    middlewaresFactories: List<FramesProcessorMiddleware.Factory>
) {
    @Serializable
    data class ProcessorConfig(
        val middlewares: List<String>,
        val parallelProcessing: Int = 8,
        val processingThrottlingMillis: Int? = 30,
        val rejectOldParallelHandling: Boolean = false
    )
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

    suspend fun availableProcessors(): Set<String> = processors.firstNotNull().keys

    suspend fun getProcessor(processorId: String): FramesProcessor? = processors.firstNotNull().get(processorId)
}

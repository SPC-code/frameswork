package space.kscience.frameswork.features.ui.panel.frames.features

import korlibs.time.DateTime
import korlibs.time.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import space.kscience.frameswork.features.frames.common.models.ByteArrayFrameData
import space.kscience.frameswork.features.frames.common.models.FramesSourceId
import space.kscience.frameswork.features.ui.panel.frames.common.features.FramesDataInfoFeature
import kotlin.collections.plus
import kotlin.collections.set
import kotlin.time.Duration

/**
 * [FramesDataInfoFeature] decorator that caches metadata queries and memoizes frame-flow handles.
 *
 * Each metadata operation has an independent, mutex-protected cache. Processor-specific and
 * source-specific results are cached separately by their identifiers; `null` processor lookups are
 * cached as well. The first request for a processor/source pair obtains a flow from [fallback];
 * subsequent requests collect that same flow object. This class does not itself turn a cold flow
 * into a hot or shared flow.
 *
 * @param fallback feature used to refresh expired metadata and provide frame flows.
 * @param cacheTime duration for which a cached metadata result remains valid.
 * @param scope retained as part of the public constructor contract; flow lifetime is controlled by
 * [fallback] and the collectors.
 */
open class CacheFramesDataInfoFeature(
    private val fallback: FramesDataInfoFeature,
    private val cacheTime: Duration = 5.seconds,
    private val scope: CoroutineScope,
) : FramesDataInfoFeature {
    /** Cached processor snapshot paired with the time at which it was loaded. */
    private var getAvailableProcessorsCache: Pair<DateTime, Set<String>>? = null

    /** Serializes reads and refreshes of the processor cache. */
    private val getAvailableProcessorsCacheMutex = Mutex()

    /**
     * Returns cached processor names, refreshing them through [fallback] after [cacheTime].
     */
    override suspend fun getAvailableProcessors(): Set<String> {
        return getAvailableProcessorsCacheMutex.withLock {
            val now = DateTime.now()
            val capturedGetAvailableProcessorsCache = getAvailableProcessorsCache

            if (capturedGetAvailableProcessorsCache == null || now - capturedGetAvailableProcessorsCache.first > cacheTime) {
                val processors = fallback.getAvailableProcessors()
                getAvailableProcessorsCache = DateTime.now() to processors
                processors
            } else {
                capturedGetAvailableProcessorsCache.second
            }
        }
    }

    /** Per-processor source snapshots paired with their load times. */
    private val getAvailableFramesSourcesCache: MutableMap<String, Pair<DateTime, Set<FramesSourceId>?>> = mutableMapOf()

    /** Serializes reads and refreshes of all source-cache entries. */
    private val getAvailableFramesSourcesCacheMutex = Mutex()

    /**
     * Returns the cached frame-source result for [processorName], refreshing that processor's entry
     * through [fallback] after [cacheTime].
     */
    override suspend fun getAvailableFramesSources(processorName: String): Set<FramesSourceId>? {
        return getAvailableFramesSourcesCacheMutex.withLock {
            val now = DateTime.now()
            val capturedGetAvailableProcessorsCache = getAvailableFramesSourcesCache[processorName]

            if (capturedGetAvailableProcessorsCache == null || now - capturedGetAvailableProcessorsCache.first > cacheTime) {
                val sources = fallback.getAvailableFramesSources(processorName)
                getAvailableFramesSourcesCache[processorName] = DateTime.now() to sources
                sources
            } else {
                capturedGetAvailableProcessorsCache.second
            }
        }
    }

    /** Memoized fallback flow objects, indexed by processor name and source-id string. */
    private val framesHalfColdFlows = MutableStateFlow<Map<String, Map<String, Flow<ByteArrayFrameData>>>>(emptyMap())

    /** Serializes lookup and creation of memoized frame-flow objects. */
    private val framesHalfColdFlowsLocker = Mutex()

    /** Obtains the fallback flow that will be memoized for [processorName] and [id]. */
    private fun allocateFramesFlow(processorName: String, id: FramesSourceId): Flow<ByteArrayFrameData> {
        return fallback.getFramesFlow(processorName, id)
    }

    /**
     * Returns a flow that collects the memoized fallback flow for [processorName] and [id].
     *
     * Repeated calls for the same pair reuse the previously allocated flow object. Reconnection and
     * sharing behavior, if any, comes from [fallback].
     */
    override fun getFramesFlow(processorName: String, id: FramesSourceId): Flow<ByteArrayFrameData> {
        return flow {
            framesHalfColdFlowsLocker.withLock {
                val existsFlows = framesHalfColdFlows.value[processorName]
                val existsFlow = existsFlows ?.get(id.string)

                if (existsFlow != null) return@withLock existsFlow

                val resultFlow = existsFlow ?: allocateFramesFlow(processorName, id)

                framesHalfColdFlows.value += processorName to ((existsFlows ?: emptyMap()) + mapOf(id.string to resultFlow))

                resultFlow
            }.collect(this)
        }
    }
}

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
 * [FramesDataInfoFeature] decorator that caches metadata queries and delegates frame streaming.
 *
 * Each metadata operation has an independent, mutex-protected cache. Processor-specific and
 * source-specific results are cached separately by their identifiers; `null` processor lookups are
 * cached as well. Frame flows bypass the cache and are returned directly by [fallback].
 *
 * @param fallback feature used to refresh expired metadata and provide frame flows.
 * @param cacheTime duration for which a cached metadata result remains valid.
 */
open class CacheFramesDataInfoFeature(
    private val fallback: FramesDataInfoFeature,
    private val cacheTime: Duration = 5.seconds,
    private val scope: CoroutineScope,
) : FramesDataInfoFeature {
    private var getAvailableProcessorsCache: Pair<DateTime, Set<String>>? = null
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

    private val getAvailableFramesSourcesCache: MutableMap<String, Pair<DateTime, Set<FramesSourceId>?>> = mutableMapOf()
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

    private val framesHalfColdFlows = MutableStateFlow<Map<String, Map<String, Flow<ByteArrayFrameData>>>>(emptyMap())
    private val framesHalfColdFlowsLocker = Mutex()

    private fun allocateFramesFlow(processorName: String, id: FramesSourceId): Flow<ByteArrayFrameData> {
        return fallback.getFramesFlow(processorName, id)
    }

    /**
     * Returns the shared, reconnecting WebSocket frame flow for [processorName] and [id].
     *
     * Repeated calls for the same pair reuse the previously allocated flow.
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

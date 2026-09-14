package space.kscience.frameswork.features.frames.common.services

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.subscribeLoggingDropExceptions
import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.micro_utils.repos.pagination.maxPagePagination
import dev.inmo.micro_utils.repos.set
import dev.inmo.micro_utils.repos.unset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import space.kscience.frameswork.features.frames.common.models.FramesSourceId

/**
 * Mutable frame-source registry persisted in a [KeyValueRepo].
 *
 * Repository insertion and removal events refresh [framesSourcesIdsListUpdatesFlow]. Connector flows
 * resolve their current value from [kvRepo] whenever the identifier set changes.
 *
 * @param scope lifecycle scope for repository subscriptions and state flows.
 * @param kvRepo repository that stores connectors by source identifier.
 */
class KVBasedFramesSourcesCollector(
    private val scope: CoroutineScope,
    private val kvRepo: KeyValueRepo<FramesSourceId, FrameSourceConnector>
) : MutableFramesSourcesCollector {
    private val _framesSourcesIdsListUpdatesFlow = MutableRedeliverStateFlow<Set<FramesSourceId>>(emptySet())
    override val framesSourcesIdsListUpdatesFlow: StateFlow<Set<FramesSourceId>> = _framesSourcesIdsListUpdatesFlow.asStateFlow()

    init {
        merge(
            kvRepo.onNewValue,
            kvRepo.onValueRemoved,
            flowOf(Unit)
        ).subscribeLoggingDropExceptions(scope) {
            _framesSourcesIdsListUpdatesFlow.value = getAvailableFramesSourcesIds()
        }
    }

    /** Returns all identifiers currently stored in [kvRepo]. */
    override suspend fun getAvailableFramesSourcesIds(): Set<FramesSourceId> {
        return kvRepo.keys(kvRepo.maxPagePagination()).results.toSet()
    }

    /** Observes the connector stored under [id], or `null` while it is absent. */
    override fun allocateConnectorFlow(id: FramesSourceId): StateFlow<FrameSourceConnector?> {
        return framesSourcesIdsListUpdatesFlow.map {
            return@map if (id in it) {
                kvRepo.get(id)
            } else {
                null
            }
        }.stateIn(scope, SharingStarted.Eagerly, null)
    }

    /**
     * Stores [frameSourceConnector], replacing any connector with the same identifier.
     *
     * @return `true` when reading the entry back yields the supplied connector.
     */
    override suspend fun addCamera(frameSourceConnector: FrameSourceConnector): Boolean {
        kvRepo.set(frameSourceConnector.id, frameSourceConnector)
        return kvRepo.get(frameSourceConnector.id) == frameSourceConnector
    }

    /**
     * Removes the connector stored under [id].
     *
     * @return `true` when the repository no longer contains [id].
     */
    override suspend fun removeCamera(id: FramesSourceId): Boolean {
        kvRepo.unset(id)
        return kvRepo.contains(id) == false
    }
}

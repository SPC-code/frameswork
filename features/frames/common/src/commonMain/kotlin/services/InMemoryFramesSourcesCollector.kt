package space.kscience.frameswork.features.frames.common.services

import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.actor
import dev.inmo.micro_utils.coroutines.runCatchingLogging
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import space.kscience.frameswork.features.common.common.utils.mapAsStateFlow
import space.kscience.frameswork.features.frames.common.models.FramesSourceId
import kotlin.collections.plus

/**
 * Mutable frame-source registry backed by process memory.
 *
 * Mutations are serialized through an actor in [scope]. All connectors from [tmpPreset] are present
 * immediately after construction, and no state is persisted across process restarts.
 *
 * @param tmpPreset connectors used to initialize the registry.
 * @param scope lifecycle scope for registry mutations and observed state flows.
 */
class InMemoryFramesSourcesCollector(
    private val tmpPreset: List<FrameSourceConnector>,
    private val scope: CoroutineScope
) : MutableFramesSourcesCollector {
    private val camerasConnectorsState = MutableRedeliverStateFlow<Map<FramesSourceId, FrameSourceConnector>>(tmpPreset.associateBy { it.id })
    override val framesSourcesIdsListUpdatesFlow: StateFlow<Set<FramesSourceId>> = camerasConnectorsState.mapAsStateFlow { it.keys }

    private val camerasModificationsActor = scope.actor<suspend () -> Unit> {
        runCatchingLogging {
            it()
        }
    }

    /** Returns a snapshot of the identifiers currently held in memory. */
    override suspend  fun getAvailableFramesSourcesIds(): Set<FramesSourceId> {
        return camerasConnectorsState.value.keys
    }

    /** Observes the connector stored under [id], or `null` while it is absent. */
    override fun allocateConnectorFlow(id: FramesSourceId): StateFlow<FrameSourceConnector?> {
        return framesSourcesIdsListUpdatesFlow.map {
            return@map if (id in it) {
                camerasConnectorsState.value[id]
            } else {
                null
            }
        }.stateIn(scope, SharingStarted.Eagerly, camerasConnectorsState.value[id])
    }

    /**
     * Adds or replaces [frameSourceConnector] under its identifier.
     *
     * @return `true` when the identifier is present after the queued mutation completes.
     */
    override suspend fun addCamera(frameSourceConnector: FrameSourceConnector): Boolean {
        val result = CompletableDeferred<Boolean>()
        camerasModificationsActor.send {
            runCatching {
                camerasConnectorsState.value += frameSourceConnector.id to frameSourceConnector
            }

            result.complete(frameSourceConnector.id in camerasConnectorsState.value.keys)
        }

        return result.await()
    }

    /**
     * Removes the connector stored under [id].
     *
     * @return `true` when the identifier is absent after the queued mutation completes.
     */
    override suspend fun removeCamera(id: FramesSourceId): Boolean {
        val result = CompletableDeferred<Boolean>()
        camerasModificationsActor.send {
            runCatching {
                camerasConnectorsState.value -= id
            }

            result.complete(id !in camerasConnectorsState.value.keys)
        }

        return result.await()
    }
}

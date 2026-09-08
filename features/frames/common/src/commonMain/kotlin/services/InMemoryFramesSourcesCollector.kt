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

    override suspend  fun getAvailableFramesSourcesIds(): Set<FramesSourceId> {
        return camerasConnectorsState.value.keys
    }

    override fun allocateConnectorFlow(id: FramesSourceId): StateFlow<FrameSourceConnector?> {
        return framesSourcesIdsListUpdatesFlow.map {
            return@map if (id in it) {
                camerasConnectorsState.value[id]
            } else {
                null
            }
        }.stateIn(scope, SharingStarted.Eagerly, camerasConnectorsState.value[id])
    }

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
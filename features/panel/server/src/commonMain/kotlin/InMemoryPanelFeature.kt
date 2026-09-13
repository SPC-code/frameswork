package space.kscience.frameswork.features.panel.server

import dev.inmo.micro_utils.coroutines.runCatchingLogging
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import space.kscience.frameswork.features.panel.common.PanelFeature
import space.kscience.frameswork.features.panel.common.models.PanelInfo

class InMemoryPanelFeature(
    private val defaultPanel: PanelInfo?,
    private val json: Json
) : PanelFeature {
    private val anyPolymorphicSerializer = PolymorphicSerializer<Any>(Any::class)

    private var panelInfo: PanelInfo? = null
    private val syncMutex = Mutex()
    override suspend fun getPanelConfig(): PanelInfo? {
        return syncMutex.withLock {
            panelInfo
        }
    }

    override suspend fun getDefaultConfig(): PanelInfo? {
        return defaultPanel
    }

    override suspend fun setPanelConfig(config: PanelInfo): Boolean {
        return syncMutex.withLock {
            panelInfo = config
            true
        }
    }
}
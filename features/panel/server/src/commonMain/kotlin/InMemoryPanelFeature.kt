package space.kscience.frameswork.features.panel.server

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import space.kscience.frameswork.features.panel.common.PanelFeature
import space.kscience.frameswork.features.panel.common.models.PanelInfo

class InMemoryPanelFeature : PanelFeature {
    private var panelInfo: PanelInfo? = null
    private val syncMutex = Mutex()
    override suspend fun getPanelConfig(): PanelInfo? {
        return syncMutex.withLock {
            panelInfo
        }
    }

    override suspend fun setPanelConfig(config: PanelInfo): Boolean {
        return syncMutex.withLock {
            panelInfo = config
            true
        }
    }
}
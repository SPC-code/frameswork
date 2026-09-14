package space.kscience.frameswork.features.panel.server

import dev.inmo.micro_utils.coroutines.runCatchingLogging
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import space.kscience.frameswork.features.panel.common.PanelFeature
import space.kscience.frameswork.features.panel.common.models.PanelInfo

/**
 * Stores the current panel configuration in this process's memory.
 *
 * The current configuration initially is `null` and is independent of [defaultPanel]. Calls that
 * read or replace the current configuration are serialized with a mutex, while the immutable default
 * is returned directly.
 *
 * @property defaultPanel optional default configuration exposed by [getDefaultConfig].
 * @property json shared JSON configuration retained for parity with storage-backed panel features. The
 * current in-memory implementation does not serialize its values.
 */
class InMemoryPanelFeature(
    private val defaultPanel: PanelInfo?,
    private val json: Json
) : PanelFeature {
    /** Polymorphic `Any` serializer retained by the class; current operations do not reference it. */
    private val anyPolymorphicSerializer = PolymorphicSerializer<Any>(Any::class)

    /** Current explicitly assigned panel configuration, or `null` before the first update. */
    private var panelInfo: PanelInfo? = null

    /** Protects reads and writes of [panelInfo]. */
    private val syncMutex = Mutex()

    /** Returns the current in-memory configuration, or `null` when none has been assigned. */
    override suspend fun getPanelConfig(): PanelInfo? {
        return syncMutex.withLock {
            panelInfo
        }
    }

    /** Returns the immutable default supplied when this feature was created. */
    override suspend fun getDefaultConfig(): PanelInfo? {
        return defaultPanel
    }

    /** Replaces the current configuration and returns `true` after the update is stored. */
    override suspend fun setPanelConfig(config: PanelInfo): Boolean {
        return syncMutex.withLock {
            panelInfo = config
            true
        }
    }
}

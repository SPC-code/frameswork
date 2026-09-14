package space.kscience.frameswork.features.panel.server

import dev.inmo.micro_utils.coroutines.runCatchingLogging
import dev.inmo.micro_utils.repos.set
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import space.kscience.frameswork.features.common.server.repos.GlobalKVRepo
import space.kscience.frameswork.features.panel.common.PanelFeature
import space.kscience.frameswork.features.panel.common.models.PanelInfo

/**
 * Persists the current panel configuration in an application-wide [GlobalKVRepo].
 *
 * The current configuration is stored as JSON under the fixed `panel_config` key. The optional
 * [defaultPanel] is kept separately and is never written to the repository.
 *
 * @property globalKVRepo repository used to read and write the shared panel configuration.
 * @property json JSON format used to encode and decode [PanelInfo].
 * @property defaultPanel optional default configuration exposed by [getDefaultConfig].
 */
class GlobalKVBindedPanelFeature(
    private val globalKVRepo: GlobalKVRepo,
    private val json: Json,
    private val defaultPanel: PanelInfo?
) : PanelFeature {
    /** Polymorphic `Any` serializer retained by the class; current operations do not reference it. */
    private val anyPolymorphicSerializer = PolymorphicSerializer<Any>(Any::class)

    /** Repository key containing the encoded current panel configuration. */
    private val key = "panel_config"

    /**
     * Reads and decodes the persisted configuration.
     *
     * @return the stored configuration, or `null` when the repository does not contain [key].
     */
    override suspend fun getPanelConfig(): PanelInfo? {
        return json.decodeFromString(
            PanelInfo.serializer(),
            globalKVRepo.get(key) ?: return null
        )
    }

    /** Returns the immutable default supplied when this feature was created. */
    override suspend fun getDefaultConfig(): PanelInfo? {
        return defaultPanel
    }

    /**
     * Encodes and stores [config], then verifies it by reading it back.
     *
     * @return `true` when the decoded stored value equals [config], otherwise `false`.
     */
    override suspend fun setPanelConfig(config: PanelInfo): Boolean {
        globalKVRepo.set(key, json.encodeToString(PanelInfo.serializer(), config))
        return getPanelConfig() == config
    }
}

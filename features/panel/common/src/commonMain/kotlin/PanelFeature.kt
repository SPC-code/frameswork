package space.kscience.frameswork.features.panel.common

import space.kscience.frameswork.features.panel.common.models.PanelInfo

/**
 * Provides access to the current and default panel layouts.
 *
 * Implementations decide where the current layout is stored and what accepting an update entails.
 */
interface PanelFeature {
    /**
     * Returns the current panel layout, or `null` when no current layout has been stored.
     */
    suspend fun getPanelConfig(): PanelInfo?

    /**
     * Returns the default panel layout, or `null` when no default is available.
     */
    suspend fun getDefaultConfig(): PanelInfo?

    /**
     * Attempts to replace the current panel layout with [config].
     *
     * @return `true` when the implementation accepted the update; otherwise `false`.
     */
    suspend fun setPanelConfig(config: PanelInfo): Boolean
}

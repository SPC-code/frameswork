package space.kscience.frameswork.features.common.web.models

import space.kscience.frameswork.features.common.web.models.ViewConfig
import kotlinx.serialization.Serializable

/**
 * Navigation-view configuration for a view that requires no parameters.
 *
 * Instances serialize as an empty object and identify themselves as `EmptyConfig` in diagnostics.
 */
@Serializable
class EmptyConfig : ViewConfig {
    /** Returns a stable diagnostic name for this empty configuration. */
    override fun toString(): String {
        return "EmptyConfig"
    }
}

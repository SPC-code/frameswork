package space.kscience.frameswork.features.common.web.models

import space.kscience.frameswork.features.common.web.models.ViewConfig
import kotlinx.serialization.Serializable

@Serializable
class EmptyConfig : ViewConfig {
    override fun toString(): String {
        return "EmptyConfig"
    }
}

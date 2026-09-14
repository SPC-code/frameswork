package space.kscience.frameswork.features.common.server.configurators

import dev.inmo.micro_utils.ktor.server.configurators.KtorApplicationConfigurator
import io.ktor.server.application.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.application.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.response.*

/**
 * Installs Ktor response compression with gzip enabled for responses of at least 1,024 bytes.
 */
class GZipConfigurator : KtorApplicationConfigurator {
    /** Installs the configured [Compression] plugin in the receiving application. */
    override fun Application.configure() {
        install(Compression) {
            gzip {
                minimumSize(minSize = 1024)
            }
        }
    }

}

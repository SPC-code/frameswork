package space.kscience.frameswork.features.common.server.configurators

import dev.inmo.micro_utils.ktor.server.configurators.KtorApplicationConfigurator
import io.ktor.server.application.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.application.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.response.*

class GZipConfigurator : KtorApplicationConfigurator {
    override fun Application.configure() {
        install(Compression) {
            gzip {
                minimumSize(minSize = 1024)
            }
        }
    }

}

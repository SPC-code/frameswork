package space.kscience.frameswork.features.common.server.configurators

import dev.inmo.micro_utils.ktor.server.configurators.KtorApplicationConfigurator
import io.ktor.server.application.Application
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.authentication

/**
 * Configures Ktor authentication from independently supplied [Element] contributions.
 *
 * This configurator only installs authentication providers. Routes must opt in to those providers
 * separately and are not created here.
 *
 * @param elements authentication contributions to apply in order.
 */
class ApplicationAuthenticationConfigurator(
    private val elements: List<Element>
) : KtorApplicationConfigurator {
    /**
     * A contribution that adds providers or other settings to Ktor's [AuthenticationConfig].
     */
    fun interface Element {
        /** Applies this contribution to the receiving authentication configuration. */
        operator fun AuthenticationConfig.invoke()
    }

    /** Installs and configures Ktor authentication with all supplied [elements]. */
    override fun Application.configure() {
        authentication {
            elements.forEach { it.apply { invoke() } }
        }
    }
}

package space.kscience.frameswork.features.panel.server

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module
import space.kscience.frameswork.features.panel.server.configurators.PanelRoutingsConfigurator

object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(space.kscience.frameswork.features.panel.common.JVMPlugin) { setupDI(config) }
        with(Plugin) { setupDI(config) }

        singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> {
            PanelRoutingsConfigurator(get())
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        space.kscience.frameswork.features.panel.common.JVMPlugin.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
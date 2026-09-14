package space.kscience.frameswork.features.ui.panel.frames.server

import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.ktor.server.configurators.ApplicationRoutingConfigurator
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module
import space.kscience.frameswork.features.ui.panel.frames.server.configurators.FramesDataInfoFeatureRoutingsConfigurator

object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(space.kscience.frameswork.features.ui.panel.frames.common.JVMPlugin) { setupDI(config) }
        with(Plugin) { setupDI(config) }

        singleWithRandomQualifier<ApplicationRoutingConfigurator.Element> { FramesDataInfoFeatureRoutingsConfigurator(get(), get()) }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        space.kscience.frameswork.features.ui.panel.frames.common.JVMPlugin.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
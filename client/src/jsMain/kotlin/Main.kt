package space.kscience.frameswork.client

import dev.inmo.micro_utils.startup.launcher.Config
import dev.inmo.micro_utils.startup.launcher.StartLauncherPlugin
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun initClient(
    plugins: List<StartPlugin>,
    defaultPlugins: List<StartPlugin> = listOf(
        ClientJSPlugin,
        space.kscience.frameswork.features.common.common.JSPlugin,
        space.kscience.frameswork.features.common.web.JSPlugin,
        space.kscience.frameswork.features.frames.web.JSPlugin,
        space.kscience.frameswork.features.panel.web.JSPlugin,
        space.kscience.frameswork.features.processor.web.JSPlugin,
        space.kscience.frameswork.features.ui.sample.JSPlugin,
        space.kscience.frameswork.features.ui.panel.JSPlugin,
        space.kscience.frameswork.features.ui.panel.frames.JSPlugin,
    )
) {
    window.addEventListener("load", {
        CoroutineScope(Dispatchers.Default).launch {
            StartLauncherPlugin.start(
                Config(
                    plugins + defaultPlugins
                )
            )
        }
    })
}

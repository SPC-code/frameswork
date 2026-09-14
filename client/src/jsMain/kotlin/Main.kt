package space.kscience.frameswork.client

import dev.inmo.micro_utils.startup.launcher.Config
import dev.inmo.micro_utils.startup.launcher.StartLauncherPlugin
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Schedules the Frameswork browser client to start when the window `load` event is emitted.
 *
 * The supplied [plugins] are placed before [defaultPlugins] in the launcher configuration. By
 * default, the latter install the client renderer and the common, Frames, panel, processor, and
 * sample UI features. The host page must provide an element whose id is `content`, which
 * [ClientJSPlugin] uses as the Compose root. Call this function before the window has finished
 * loading; the function only registers a load listener and does not start the launcher immediately.
 *
 * @param plugins application-specific startup plugins to prepend to the launcher configuration.
 * @param defaultPlugins client infrastructure and feature plugins to append after [plugins].
 */
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

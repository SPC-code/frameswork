package space.kscience.frameswork.client

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import dev.inmo.micro_utils.coroutines.MutableRedeliverStateFlow
import dev.inmo.micro_utils.coroutines.compose.StyleSheetsAggregator
import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.core.NavigationChain
import dev.inmo.navigation.core.NavigationNodeFactory
import dev.inmo.navigation.core.repo.NavigationConfigsRepo
import space.kscience.frameswork.features.common.web.models.ViewConfig
import kotlinx.serialization.json.JsonObject
import org.jetbrains.compose.web.css.Style
import org.jetbrains.compose.web.renderComposable
import org.koin.core.Koin
import org.koin.core.module.Module
import space.kscience.frameswork.features.common.web.ui.FlotViewStyleSheet

object ClientJSPlugin : StartPlugin {
    val currentDrawingBlock = MutableRedeliverStateFlow<@Composable () -> Unit>({})
    override fun Module.setupDI(config: JsonObject) {
        with(ClientPlugin) { setupDI(config) }

        single {
            { drawable: @Composable () -> Unit ->
                currentDrawingBlock.value = drawable
            }
        }

        single<NavigationConfigsRepo<ViewConfig>> {
            NavigationConfigsRepo.InMemory<ViewConfig>()
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        ClientPlugin.startPlugin(koin)
        super.startPlugin(koin)
        StyleSheetsAggregator.addStyleSheet(FlotViewStyleSheet)
        renderComposable("content") {
            StyleSheetsAggregator.draw()
            currentDrawingBlock.collectAsState().value.invoke()
        }
    }
}

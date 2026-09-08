package space.kscience.frameswork.features.common.web

import dev.inmo.micro_utils.coroutines.runCatchingLogging
import dev.inmo.micro_utils.koin.getAllDistinct
import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import dev.inmo.navigation.core.NavigationNode
import dev.inmo.navigation.core.NavigationNodeFactory
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineFactory
import space.kscience.frameswork.features.common.web.models.ViewConfig
import kotlinx.serialization.json.JsonObject
import org.koin.core.Koin
import org.koin.core.module.Module
import space.kscience.frameswork.features.common.web.configurators.DefaultUrlHttpClientConfigurator
import space.kscience.frameswork.features.common.web.configurators.HttpClientConfigurator
import space.kscience.frameswork.features.common.web.configurators.SerializationConfigurator
import space.kscience.frameswork.features.common.web.configurators.WebSocketsConfigurator
import space.kscience.frameswork.features.common.web.models.EmptyConfig

object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        singleWithRandomQualifier<NavigationNodeFactory<ViewConfig>> {
            NavigationNodeFactory.Typed(
                EmptyConfig::class
            ) { navigationChain, config ->
                NavigationNode.Empty(navigationChain, config)
            }
        }

        singleWithRandomQualifier<HttpClientConfigurator> { SerializationConfigurator(get()) }
        singleWithRandomQualifier<HttpClientConfigurator> { DefaultUrlHttpClientConfigurator(::getUrl) }
        singleWithRandomQualifier<HttpClientConfigurator> { WebSocketsConfigurator(get()) }

        single {
            val configurators = getAllDistinct<HttpClientConfigurator>()
            val predefinedEngine = getOrNull<HttpClientEngineFactory<*>>()

            val configuringCallback: HttpClientConfig<*>.() -> Unit = {
                configurators.forEach {
                    with (it) {
                        runCatchingLogging {
                            configure()
                        }
                    }
                }
            }

            if (predefinedEngine == null) {
                HttpClient(configuringCallback)
            } else {
                HttpClient(predefinedEngine, configuringCallback)
            }
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}

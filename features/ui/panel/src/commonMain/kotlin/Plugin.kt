package space.kscience.frameswork.features.ui.panel

import dev.inmo.micro_utils.colors.common.HEXAColor
import dev.inmo.micro_utils.colors.green
import dev.inmo.micro_utils.colors.red
import dev.inmo.micro_utils.koin.getAllDistinct
import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.Koin
import org.koin.core.module.Module
import space.kscience.frameswork.features.common.web.models.ViewConfig
import space.kscience.frameswork.features.panel.common.PanelFeature
import space.kscience.frameswork.features.panel.common.models.PanelInfo
import space.kscience.frameswork.features.panel.common.models.PanelItemInfo
import space.kscience.frameswork.features.ui.panel.ui.PanelModel
import space.kscience.frameswork.features.ui.panel.ui.PanelViewConfig
import space.kscience.frameswork.features.ui.panel.ui.PanelViewModel
import space.kscience.frameswork.features.ui.sample.ui.SampleViewConfig
import kotlin.random.Random
import kotlin.random.nextUInt

object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        singleWithRandomQualifier {
            SerializersModule {
                polymorphic(Any::class, PanelViewConfig::class, PanelViewConfig.serializer())
                polymorphic(ViewConfig::class, PanelViewConfig::class, PanelViewConfig.serializer())
            }
        }
        factory {
            PanelViewModel(node = it.get(), model = get(), panelViewConfigProviders = getAllDistinct(), json = get())
        }
        single<PanelModel> {
            val panelFeature = get<PanelFeature>()
            val updatesFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
            val json = get<Json>()
            object : PanelModel {
                override val decodingJson: Json
                    get() = json
                override fun getPanelInfoFlow(): Flow<PanelInfo> {
                    return merge(updatesFlow, flowOf(Unit)).map {
                        panelFeature.getPanelConfig() ?: PanelInfo(
                            horizontalSlots = 3,
                            verticalSlots = 3,
                            items = listOf(
                                PanelItemInfo(
                                    x = 0,
                                    y = 0,
                                    width = 1,
                                    height = 1,
                                    config = SampleViewConfig(
                                        HEXAColor(Random.nextUInt(0xffffffffu)),
                                        text = "0, 0"
                                    ),
                                    json
                                ),
                                PanelItemInfo(
                                    x = 0,
                                    y = 1,
                                    width = 1,
                                    height = 1,
                                    config = SampleViewConfig(
                                        HEXAColor(Random.nextUInt(0xffffffffu)),
                                        text = "0, 1"
                                    ),
                                    json
                                ),
                                PanelItemInfo(
                                    x = 0,
                                    y = 2,
                                    width = 1,
                                    height = 1,
                                    config = SampleViewConfig(
                                        HEXAColor(Random.nextUInt(0xffffffffu)),
                                        text = "0, 2"
                                    ),
                                    json
                                ),
                                PanelItemInfo(
                                    x = 1,
                                    y = 0,
                                    width = 1,
                                    height = 1,
                                    config = SampleViewConfig(
                                        HEXAColor(Random.nextUInt(0xffffffffu)),
                                        text = "1, 0"
                                    ),
                                    json
                                ),
                                PanelItemInfo(
                                    x = 1,
                                    y = 1,
                                    width = 1,
                                    height = 1,
                                    config = SampleViewConfig(
                                        HEXAColor(Random.nextUInt(0xffffffffu)),
                                        text = "1, 1"
                                    ),
                                    json
                                ),
                                PanelItemInfo(
                                    x = 1,
                                    y = 2,
                                    width = 1,
                                    height = 1,
                                    config = SampleViewConfig(
                                        HEXAColor(Random.nextUInt(0xffffffffu)),
                                        text = "1, 2"
                                    ),
                                    json
                                ),
                                PanelItemInfo(
                                    x = 2,
                                    y = 0,
                                    width = 1,
                                    height = 1,
                                    config = SampleViewConfig(
                                        HEXAColor(Random.nextUInt(0xffffffffu)),
                                        text = "2, 0"
                                    ),
                                    json
                                ),
                                PanelItemInfo(
                                    x = 2,
                                    y = 1,
                                    width = 1,
                                    height = 1,
                                    config = SampleViewConfig(
                                        HEXAColor(Random.nextUInt(0xffffffffu)),
                                        text = "2, 1"
                                    ),
                                    json
                                ),
                                PanelItemInfo(
                                    x = 2,
                                    y = 2,
                                    width = 1,
                                    height = 1,
                                    config = SampleViewConfig(
                                        HEXAColor(Random.nextUInt(0xffffffffu)),
                                        text = "2, 2"
                                    ),
                                    json
                                ),
                            )
                        )
                    }
                }

                override suspend fun updatePanelInfo(info: PanelInfo) {
                    panelFeature.setPanelConfig(info)
                    updatesFlow.emit(Unit)
                }
            }
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
    }
}
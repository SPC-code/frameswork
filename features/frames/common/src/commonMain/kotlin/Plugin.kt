package space.kscience.frameswork.features.frames.common

import dev.inmo.micro_utils.koin.getAllDistinct
import dev.inmo.micro_utils.koin.singleWithRandomQualifier
import dev.inmo.micro_utils.meta.MetaContainer
import dev.inmo.micro_utils.startup.plugin.StartPlugin
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.modules.SerializersModule
import org.koin.core.module.Module
import space.kscience.frameswork.features.frames.common.models.FrameReceiveTimestamp
import space.kscience.frameswork.features.frames.common.models.FrameSourceHeight
import space.kscience.frameswork.features.frames.common.models.FramesSourceIdMeta
import space.kscience.frameswork.features.frames.common.models.FrameSourceTimestampMicroseconds
import space.kscience.frameswork.features.frames.common.models.FrameSourceWidth
import space.kscience.frameswork.features.frames.common.models.FramesSourceId
import space.kscience.frameswork.features.frames.common.services.DefaultFramesCollector
import space.kscience.frameswork.features.frames.common.services.FramesCollector
import space.kscience.frameswork.features.frames.common.services.InMemoryFramesSourcesCollector

object Plugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        single {
            DefaultFramesCollector(
                framesCollector = get(),
                scope = get()
            )
        }

        single<FramesCollector> {
            get<DefaultFramesCollector>()
        }

        single {
            InMemoryFramesSourcesCollector(
                tmpPreset = getAllDistinct(),
                scope = get()
            )
        }

        singleWithRandomQualifier {
            SerializersModule {
                polymorphic(Any::class, FramesSourceId::class, FramesSourceId.serializer())

                polymorphic(Any::class, FramesSourceIdMeta::class, FramesSourceIdMeta.serializer())
                polymorphic(MetaContainer.Key::class, FramesSourceIdMeta::class, FramesSourceIdMeta.serializer())
                polymorphic(Any::class, FrameSourceWidth::class, FrameSourceWidth.serializer())
                polymorphic(MetaContainer.Key::class, FrameSourceWidth::class, FrameSourceWidth.serializer())
                polymorphic(Any::class, FrameSourceHeight::class, FrameSourceHeight.serializer())
                polymorphic(MetaContainer.Key::class, FrameSourceHeight::class, FrameSourceHeight.serializer())
                polymorphic(Any::class, FrameReceiveTimestamp::class, FrameReceiveTimestamp.serializer())
                polymorphic(MetaContainer.Key::class, FrameReceiveTimestamp::class, FrameReceiveTimestamp.serializer())
                polymorphic(Any::class, FrameSourceTimestampMicroseconds::class, FrameSourceTimestampMicroseconds.serializer())
                polymorphic(MetaContainer.Key::class, FrameSourceTimestampMicroseconds::class, FrameSourceTimestampMicroseconds.serializer())
            }
        }
    }
}

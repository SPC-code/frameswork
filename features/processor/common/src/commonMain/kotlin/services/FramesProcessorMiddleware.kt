package space.kscience.frameswork.features.processor.common.services

import kotlinx.serialization.Serializable
import space.kscience.frameswork.features.frames.common.models.FrameData
import kotlin.jvm.JvmInline

interface FramesProcessorMiddleware {
    interface Factory {
        @Serializable
        @JvmInline
        value class Id(val string: String)

        val id: Id

        @Serializable
        class Simple(
            override val id: Id,
            private val middleware: FramesProcessorMiddleware
        ) : Factory {
            override suspend fun createMiddleware(): FramesProcessorMiddleware {
                return middleware
            }
        }

        suspend fun createMiddleware(): FramesProcessorMiddleware
    }
    suspend fun process(frame: FrameData): FrameData
}

package space.kscience.frameswork.features.processor.common.services

import kotlinx.serialization.Serializable
import space.kscience.frameswork.features.frames.common.models.FrameData
import kotlin.jvm.JvmInline

/** Transforms individual [FrameData] values as one stage of a frame-processing pipeline. */
interface FramesProcessorMiddleware {
    /** Creates middleware instances that can be selected by a stable [id]. */
    interface Factory {
        /**
         * Stable serialized identifier of a middleware factory.
         *
         * @property string Identifier value used in processor configuration.
         */
        @Serializable
        @JvmInline
        value class Id(val string: String)

        /** Identifier used to refer to this factory from processor configuration. */
        val id: Id

        /**
         * Factory that always returns an existing [middleware] instance.
         *
         * @property id Identifier exposed by this factory.
         * @param middleware Middleware instance returned by [createMiddleware].
         */
        @Serializable
        class Simple(
            override val id: Id,
            private val middleware: FramesProcessorMiddleware
        ) : Factory {
            /** Returns the middleware instance supplied when this factory was constructed. */
            override suspend fun createMiddleware(): FramesProcessorMiddleware {
                return middleware
            }
        }

        /**
         * Creates the middleware represented by this factory.
         *
         * @return A middleware instance ready to process frames.
         */
        suspend fun createMiddleware(): FramesProcessorMiddleware
    }

    /**
     * Processes [frame] and returns the value to pass to the next pipeline stage.
     *
     * Implementations may return [frame] unchanged.
     *
     * @param frame Input frame for this stage.
     * @return The frame to pass to the next middleware or consumer.
     */
    suspend fun process(frame: FrameData): FrameData
}

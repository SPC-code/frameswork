package space.kscience.frameswork.features.frames.common.models

import dev.inmo.micro_utils.meta.MetaContainer

interface FrameData {
    val meta: MetaContainer
    suspend fun toByteArray(): ByteArray

    fun copyWithModifiedMeta(block: MetaContainer.Builder.() -> Unit): FrameData
}

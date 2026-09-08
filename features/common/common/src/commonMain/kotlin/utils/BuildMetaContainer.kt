package space.kscience.frameswork.features.common.common.utils

import dev.inmo.micro_utils.meta.MetaContainer

fun MetaContainer.modified(
    block: MetaContainer.Builder.() -> Unit
): MetaContainer {
    val builder = MetaContainer.Builder(map.toMutableMap())
    builder.block()
    return builder.build()
}

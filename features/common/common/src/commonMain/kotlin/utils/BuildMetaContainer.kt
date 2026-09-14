package space.kscience.frameswork.features.common.common.utils

import dev.inmo.micro_utils.meta.MetaContainer

/**
 * Creates a copy of this metadata container and applies [block] to the copy's builder.
 *
 * Entries not changed by [block] are retained in the returned container.
 *
 * @receiver The source metadata container.
 * @param block Mutations to apply while building the copied container.
 * @return A newly built metadata container with the requested changes.
 */
fun MetaContainer.modified(
    block: MetaContainer.Builder.() -> Unit
): MetaContainer {
    val builder = MetaContainer.Builder(map.toMutableMap())
    builder.block()
    return builder.build()
}

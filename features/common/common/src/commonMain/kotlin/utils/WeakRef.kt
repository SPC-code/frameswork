package space.kscience.frameswork.features.common.common.utils

/**
 * A platform-specific weak reference that does not keep [instance] alive.
 *
 * @param T Type of the referenced object.
 * @param instance Initial referent.
 */
expect class WeakRef<T>(instance: T)

/**
 * Reads the current referent of this weak reference.
 *
 * @receiver The weak reference to inspect.
 * @return The referenced object, or `null` after it has been reclaimed.
 */
expect fun <T> WeakRef<T>.get(): T?

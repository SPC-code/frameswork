package space.kscience.frameswork.features.common.common.utils

/**
 * JavaScript implementation backed by the platform's native `WeakRef` object.
 *
 * @param T Type of the referenced object.
 * @param instance Initial referent.
 */
actual external class WeakRef<T> actual constructor(instance: T) {
    /**
     * Dereferences this JavaScript weak reference.
     *
     * @return The referenced object, or `null` after it has been reclaimed.
     */
    fun deref(): T?
}

/**
 * Returns the current JavaScript referent, if it is still alive.
 *
 * @receiver The weak reference to inspect.
 * @return The referenced object, or `null` after it has been reclaimed.
 */
actual fun <T> WeakRef<T>.get(): T? = deref()

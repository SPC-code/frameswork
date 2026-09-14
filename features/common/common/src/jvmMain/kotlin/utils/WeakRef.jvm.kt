package space.kscience.frameswork.features.common.common.utils

import java.lang.ref.WeakReference

/**
 * JVM implementation backed by [WeakReference].
 *
 * @param T Type of the referenced object.
 */
actual typealias WeakRef<T> = WeakReference<T>

/**
 * Returns the current JVM referent, if it is still alive.
 *
 * @receiver The weak reference to inspect.
 * @return The referenced object, or `null` after it has been reclaimed.
 */
actual fun <T> WeakRef<T>.get(): T? = get()

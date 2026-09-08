package space.kscience.frameswork.features.common.common.utils

actual external class WeakRef<T> actual constructor(instance: T) {
    fun deref(): T?
}
actual fun <T> WeakRef<T>.get(): T? = deref()

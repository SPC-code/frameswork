package space.kscience.frameswork.features.common.common.utils

expect class WeakRef<T>(instance: T)

expect fun <T> WeakRef<T>.get(): T?

package space.kscience.frameswork.features.common.common.utils

import java.lang.ref.WeakReference

actual typealias WeakRef<T> = WeakReference<T>
actual fun <T> WeakRef<T>.get(): T? = get()

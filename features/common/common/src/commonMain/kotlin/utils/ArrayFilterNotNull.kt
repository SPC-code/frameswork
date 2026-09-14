package space.kscience.frameswork.features.common.common.utils

/**
 * Copies the non-null elements of this array into a new, non-nullable array while preserving their order.
 *
 * @receiver The array whose `null` elements should be discarded.
 * @return A new array containing only non-null elements from this array.
 */
inline fun <reified T> Array<T?>.filterNotNullInArray(): Array<T> {
    val nonNullResultsAmount = count { it != null }
    var i = 0
    return Array(nonNullResultsAmount) {
        var currentValue = get(i)
        while (currentValue == null) {
            i++
            currentValue = get(i)
        }
        i++
        currentValue
    }
}

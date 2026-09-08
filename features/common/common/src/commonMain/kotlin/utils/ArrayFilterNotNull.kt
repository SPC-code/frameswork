package space.kscience.frameswork.features.common.common.utils

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

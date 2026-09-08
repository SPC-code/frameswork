package space.kscience.frameswork.features.common.common.utils

fun ByteArray.toInt(): Int {
    var result = 0
    for (i in size - 1 downTo (maxOf(0, size - 5))) {
        val currentInt = get(i).toUByte().toInt().shl(8 * (size - 1 - i))
        result = result or currentInt
    }
    return result
}

fun Int.toByteArray(): ByteArray {
    val bytes = ByteArray(4)
    bytes[0] = shr(24).toByte()
    bytes[1] = shr(16).toByte()
    bytes[2] = shr(8).toByte()
    bytes[3] = toByte()
    return bytes
}

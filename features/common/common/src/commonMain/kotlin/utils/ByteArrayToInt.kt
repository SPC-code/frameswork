package space.kscience.frameswork.features.common.common.utils

/**
 * Reconstructs an [Int] from bytes in big-endian order.
 *
 * This function is intended for four-byte arrays such as those produced by [Int.toByteArray].
 *
 * @receiver The bytes to combine, with the most-significant byte first.
 * @return The integer represented by this byte array.
 */
fun ByteArray.toInt(): Int {
    var result = 0
    for (i in size - 1 downTo (maxOf(0, size - 5))) {
        val currentInt = get(i).toUByte().toInt().shl(8 * (size - 1 - i))
        result = result or currentInt
    }
    return result
}

/**
 * Encodes this integer as a four-byte array in big-endian order.
 *
 * @receiver The integer to encode.
 * @return Four bytes ordered from the most-significant byte to the least-significant byte.
 */
fun Int.toByteArray(): ByteArray {
    val bytes = ByteArray(4)
    bytes[0] = shr(24).toByte()
    bytes[1] = shr(16).toByte()
    bytes[2] = shr(8).toByte()
    bytes[3] = toByte()
    return bytes
}

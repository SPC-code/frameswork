package space.kscience.frameswork.features.common.common.utils

import kotlin.test.Test
import kotlin.test.assertEquals

/** Verifies the big-endian integer and byte-array conversion helpers. */
class ByteArrayToIntTests {
    /** Verifies that a negative integer is split into four big-endian bytes. */
    @Test
    fun testIntToByteArray() {
        val targetInt: Int = -1070593021
        val byteArray = targetInt.toByteArray()

        assertEquals(byteArray[0], 0b11000000.toByte())
        assertEquals(byteArray[1], 0b00110000.toByte())
        assertEquals(byteArray[2], 0b00001100.toByte())
        assertEquals(byteArray[3], 0b00000011.toByte())
    }

    /** Verifies that four big-endian bytes reconstruct a negative integer. */
    @Test
    fun testByteArrayToInt() {
        val targetByteArray = byteArrayOf(0b11000000.toByte(), 0b00110000, 0b00001100, 0b00000011)
        val targetInt = targetByteArray.toInt()

        assertEquals(targetInt, -1070593021)
    }

    /** Verifies encoding when the least-significant byte has its leading bit set. */
    @Test
    fun testIntToByteArrayWithFirstByteLeadingBit() {
        val targetInt: Int = 53480640
        val byteArray = targetInt.toByteArray()

        assertEquals(byteArray[0], 0b00000011.toByte())
        assertEquals(byteArray[1], 0b00110000.toByte())
        assertEquals(byteArray[2], 0b00001100.toByte())
        assertEquals(byteArray[3], 0b11000000.toByte())
    }

    /** Verifies decoding when the least-significant byte has its leading bit set. */
    @Test
    fun testByteArrayToIntWithFirstByteLeadingBit() {
        val targetByteArray = byteArrayOf(0b00000011, 0b00110000, 0b00001100, 0b11000000.toByte())
        val targetInt = targetByteArray.toInt()

        assertEquals(targetInt, 53480640)
    }
}

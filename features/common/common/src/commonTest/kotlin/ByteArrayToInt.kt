package space.kscience.frameswork.features.common.common.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class ByteArrayToIntTests {
    @Test
    fun testIntToByteArray() {
        val targetInt: Int = -1070593021
        val byteArray = targetInt.toByteArray()

        assertEquals(byteArray[0], 0b11000000.toByte())
        assertEquals(byteArray[1], 0b00110000.toByte())
        assertEquals(byteArray[2], 0b00001100.toByte())
        assertEquals(byteArray[3], 0b00000011.toByte())
    }

    @Test
    fun testByteArrayToInt() {
        val targetByteArray = byteArrayOf(0b11000000.toByte(), 0b00110000, 0b00001100, 0b00000011)
        val targetInt = targetByteArray.toInt()

        assertEquals(targetInt, -1070593021)
    }

    @Test
    fun testIntToByteArrayWithFirstByteLeadingBit() {
        val targetInt: Int = 53480640
        val byteArray = targetInt.toByteArray()

        assertEquals(byteArray[0], 0b00000011.toByte())
        assertEquals(byteArray[1], 0b00110000.toByte())
        assertEquals(byteArray[2], 0b00001100.toByte())
        assertEquals(byteArray[3], 0b11000000.toByte())
    }

    @Test
    fun testByteArrayToIntWithFirstByteLeadingBit() {
        val targetByteArray = byteArrayOf(0b00000011, 0b00110000, 0b00001100, 0b11000000.toByte())
        val targetInt = targetByteArray.toInt()

        assertEquals(targetInt, 53480640)
    }
}

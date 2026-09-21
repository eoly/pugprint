package com.example.pugprint.printer

import com.example.pugprint.printer.Fixtures.hex
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class RasterBlockTest {
    private val black = ByteArray(PrinterSpec.BYTES_PER_ROW) { 0xFF.toByte() }
    private val white = ByteArray(PrinterSpec.BYTES_PER_ROW)

    @Test
    fun `one 384-dot row encodes as the confirmed 56-byte block`() {
        val block = RasterBlock.encode(listOf(black))

        assertEquals(56, block.size)
        assertEquals("1d 76 30 00 30 00 01 00", hex(block.copyOf(RasterBlock.HEADER_SIZE)))
        assertArrayEquals(black, block.copyOfRange(RasterBlock.HEADER_SIZE, block.size))
    }

    @Test
    fun `row count and width are little-endian 16-bit`() {
        val rows = List(300) { white }
        val block = RasterBlock.encode(rows)
        assertEquals("1d 76 30 00 30 00 2c 01", hex(block.copyOf(RasterBlock.HEADER_SIZE)))

        val wide = RasterBlock.encode(listOf(ByteArray(0x0102)))
        assertEquals("1d 76 30 00 02 01 01 00", hex(wide.copyOf(RasterBlock.HEADER_SIZE)))
    }

    @Test
    fun `rows are concatenated in order`() {
        val block = RasterBlock.encode(listOf(black, white))

        assertEquals(RasterBlock.HEADER_SIZE + 2 * PrinterSpec.BYTES_PER_ROW, block.size)
        assertArrayEquals(black, block.copyOfRange(8, 56))
        assertArrayEquals(white, block.copyOfRange(56, 104))
    }

    @Test
    fun `split defaults to one row per block and caps at four`() {
        val rows = List(10) { white }

        assertEquals(10, RasterBlock.split(rows).size)
        assertEquals(3, RasterBlock.split(rows, rowsPerBlock = 4).size)
        assertEquals("1d 76 30 00 30 00 02 00", hex(RasterBlock.split(rows, rowsPerBlock = 4).last().copyOf(8)))
        assertThrows(IllegalArgumentException::class.java) { RasterBlock.split(rows, rowsPerBlock = 5) }
        assertThrows(IllegalArgumentException::class.java) { RasterBlock.split(rows, rowsPerBlock = 0) }
    }

    @Test
    fun `rejects empty input and ragged rows`() {
        assertThrows(IllegalArgumentException::class.java) { RasterBlock.encode(emptyList()) }
        assertThrows(IllegalArgumentException::class.java) { RasterBlock.encode(listOf(black, ByteArray(47))) }
        assertThrows(IllegalArgumentException::class.java) { RasterBlock.encode(listOf(ByteArray(0))) }
    }
}

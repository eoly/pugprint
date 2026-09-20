package com.example.pugprint.imaging

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MonoBitmapTest {
    @Test
    fun `packs msb-first with 1 = black`() {
        // 10x2: row 0 = first pixel black only; row 1 = last pixel black only.
        val pixels = BooleanArray(20)
        pixels[0] = true
        pixels[19] = true

        val bmp = MonoBitmap.fromPixels(width = 10, height = 2, black = pixels)

        assertEquals(2, bmp.bytesPerRow)
        assertArrayEquals(byteArrayOf(0x80.toByte(), 0x00), bmp.row(0))
        assertArrayEquals(byteArrayOf(0x00, 0x40), bmp.row(1))
        assertTrue(bmp.isBlack(0, 0))
        assertFalse(bmp.isBlack(1, 0))
        assertTrue(bmp.isBlack(9, 1))
    }

    @Test
    fun `a solid 384-wide row is 48 bytes of 0xFF`() {
        val bmp = MonoBitmap.fromPixels(384, 1, BooleanArray(384) { true })

        assertEquals(48, bmp.bytesPerRow)
        assertArrayEquals(ByteArray(48) { 0xFF.toByte() }, bmp.packed)
    }

    @Test
    fun `rejects mismatched buffer sizes`() {
        assertThrows(IllegalArgumentException::class.java) { MonoBitmap(8, 2, ByteArray(1)) }
        assertThrows(IllegalArgumentException::class.java) { MonoBitmap.fromPixels(8, 1, BooleanArray(7)) }
    }
}

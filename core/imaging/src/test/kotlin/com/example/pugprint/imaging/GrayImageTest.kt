package com.example.pugprint.imaging

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class GrayImageTest {
    /** 3×2, values are `10 * x + y` so every pixel is distinct. */
    private val image = GrayImage.generate(3, 2) { x, y -> 10 * x + y }

    @Test
    fun `luma is Rec 601 in integer form`() {
        assertEquals(0, GrayImage.lumaOf(0, 0, 0))
        assertEquals(255, GrayImage.lumaOf(255, 255, 255))
        assertEquals(75 * 255 shr 7, GrayImage.lumaOf(0, 255, 0))
        assertEquals(38 * 255 shr 7, GrayImage.lumaOf(255, 0, 0))
    }

    @Test
    fun `generate clamps and get reads back`() {
        val clamped = GrayImage.generate(2, 1) { x, _ -> if (x == 0) -5 else 300 }
        assertEquals(0, clamped[0, 0])
        assertEquals(255, clamped[1, 0])
        assertEquals(21, image[2, 1])
        assertThrows(IllegalArgumentException::class.java) { image[3, 0] }
        assertThrows(IllegalArgumentException::class.java) { GrayImage(2, 2, ByteArray(3)) }
    }

    @Test
    fun `rotates clockwise, half and counter-clockwise`() {
        val cw = image.rotated(Rotation.CLOCKWISE_90)
        assertEquals(2, cw.width)
        assertEquals(3, cw.height)
        // Top-left of the source ends up top-right; bottom-left ends up top-left.
        assertEquals(0, cw[1, 0])
        assertEquals(1, cw[0, 0])
        assertEquals(20, cw[1, 2])
        assertEquals(21, cw[0, 2])

        val half = image.rotated(Rotation.HALF)
        assertEquals(21, half[0, 0])
        assertEquals(0, half[2, 1])

        val ccw = image.rotated(Rotation.COUNTER_CLOCKWISE_90)
        assertEquals(0, ccw[0, 2])
        assertEquals(20, ccw[0, 0])
        assertEquals(21, ccw[1, 0])

        assertSame(image, image.rotated(Rotation.NONE))
        val fourTurns = (1..4).fold(image) { acc, _ -> acc.rotated(Rotation.CLOCKWISE_90) }
        assertArrayEquals(image.luma, fourTurns.luma)
    }

    @Test
    fun `quarter turns cycle`() {
        assertEquals(Rotation.CLOCKWISE_90, Rotation.NONE.plusQuarterTurn())
        assertEquals(Rotation.NONE, Rotation.COUNTER_CLOCKWISE_90.plusQuarterTurn())
    }

    @Test
    fun `crops a window and rejects one outside the image`() {
        val crop = image.cropped(CropRect(1, 0, 2, 2))
        assertEquals(2, crop.width)
        assertEquals(2, crop.height)
        assertEquals(10, crop[0, 0])
        assertEquals(21, crop[1, 1])
        assertSame(image, image.cropped(CropRect(0, 0, 3, 2)))
        assertThrows(IllegalArgumentException::class.java) { image.cropped(CropRect(2, 0, 2, 1)) }
        assertThrows(IllegalArgumentException::class.java) { CropRect(0, 0, 0, 1) }
    }

    @Test
    fun `downscaling by two averages each 2x2 block`() {
        val source = GrayImage.generate(4, 4) { x, y -> if ((x + y) % 2 == 0) 0 else 255 }
        val half = source.scaled(2, 2)
        assertEquals(2, half.width)
        assertArrayEquals(ByteArray(4) { 128.toByte() }, half.luma)
    }

    @Test
    fun `scaledToWidth keeps the aspect ratio and rounds the height`() {
        val source = GrayImage.generate(768, 500) { x, _ -> x % 256 }
        val scaled = source.scaledToWidth(384)
        assertEquals(384, scaled.width)
        assertEquals(250, scaled.height)
        assertEquals(1000 * 384 / 768, GrayImage.generate(768, 1000) { _, _ -> 0 }.scaledToWidth(384).height)
        assertEquals(1, GrayImage.generate(1000, 1) { _, _ -> 0 }.scaledToWidth(384).height)
        assertSame(source, source.scaledToWidth(768))
    }

    @Test
    fun `non-integer ratios weight partial coverage`() {
        // 3 → 2: destination 0 covers source 0 fully and half of 1; destination 1 the rest.
        val source = GrayImage.generate(3, 1) { x, _ -> intArrayOf(0, 90, 240)[x] }
        val scaled = source.scaled(2, 1)
        assertEquals(30, scaled[0, 0])
        assertEquals(190, scaled[1, 0])
    }

    @Test
    fun `upscaling repeats pixels`() {
        val source = GrayImage.generate(2, 1) { x, _ -> if (x == 0) 0 else 200 }
        val scaled = source.scaled(4, 1)
        assertArrayEquals(byteArrayOf(0, 0, 200.toByte(), 200.toByte()), scaled.luma)
    }
}

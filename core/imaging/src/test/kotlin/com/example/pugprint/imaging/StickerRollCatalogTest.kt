package com.example.pugprint.imaging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/** The rules every roll in [StickerRollCatalog.all] must follow, and the standard roll's measured placement. */
class StickerRollCatalogTest {
    @Test
    fun `ids are unique, lowercase and never blank`() {
        val ids = StickerRollCatalog.all.map { it.id }
        assertEquals(ids.toSet().size, ids.size, "duplicate roll ids in $ids")
        ids.forEach { assertTrue(it.isNotBlank() && it == it.lowercase() && ' ' !in it, "bad roll id '$it'") }
    }

    @Test
    fun `the default is in the catalog and unknown ids fall back to it`() {
        assertTrue(StickerRollCatalog.default in StickerRollCatalog.all)
        assertSame(StickerRollCatalog.default, StickerRollCatalog.byId("papyrus"))
        StickerRollCatalog.all.forEach { assertSame(it, StickerRollCatalog.byId(it.id)) }
    }

    @TestFactory
    fun `every roll has a name and room to print`(): List<DynamicTest> =
        StickerRollCatalog.all.map { roll ->
            DynamicTest.dynamicTest(roll.id) {
                assertTrue(roll.displayName.isNotBlank())
                assertTrue(roll.contentWidth in 1..StickerRoll.HEAD_DOTS)
                assertTrue(roll.contentHeight > 0)
                if (roll.isLabel) assertTrue(roll.contentHeight <= StickerRoll.HEAD_DOTS)
            }
        }

    @Test
    fun `the standard square roll prints a 365-dot square with white on the right and top`() {
        val roll = StickerRollCatalog.SquareStandard
        assertTrue(roll.isLabel)
        assertEquals(365, roll.contentWidth)
        assertEquals(365, roll.contentHeight)

        val black = MonoBitmap.fromPixels(365, 365, BooleanArray(365 * 365) { true })
        val placed = roll.place(black)
        assertEquals(384, placed.width)
        assertEquals(384, placed.height)
        // top margin and right inset are white; the picture is black; nothing below it on this roll.
        assertFalse(placed.isBlack(0, 0))
        assertFalse(placed.isBlack(383, 200))
        assertFalse(placed.isBlack(365, 200))
        assertTrue(placed.isBlack(0, 19))
        assertTrue(placed.isBlack(364, 383))
        Pbm.assertMatchesGolden("roll_square_placement", placed)
    }

    @Test
    fun `a shorter picture on a label is padded to a whole label`() {
        val roll = StickerRollCatalog.SquareStandard
        val short = MonoBitmap.fromPixels(365, 100, BooleanArray(365 * 100) { true })
        val placed = roll.place(short)
        assertEquals(384, placed.height)
        assertTrue(placed.isBlack(10, 19 + 50))
        assertFalse(placed.isBlack(10, 19 + 100))
    }

    @Test
    fun `the plain roll is untouched and any height goes`() {
        val roll = StickerRollCatalog.Continuous
        assertFalse(roll.isLabel)
        assertEquals(384, roll.contentWidth)
        assertEquals(ImagePipeline.MAX_ROWS, roll.contentHeight)
        val tall = MonoBitmap.fromPixels(384, 1000, BooleanArray(384 * 1000) { false })
        assertSame(tall, roll.place(tall))
    }

    @Test
    fun `place refuses the wrong width`() {
        val wrong = MonoBitmap.fromPixels(384, 10, BooleanArray(384 * 10) { false })
        assertThrows(IllegalArgumentException::class.java) { StickerRollCatalog.SquareStandard.place(wrong) }
    }
}

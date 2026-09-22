package com.example.pugprint.imaging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Golden test against `src/test/resources/test_pattern.pbm`. Re-record after an intentional
 * change with `./gradlew :core:imaging:test -Dpugprint.recordGoldens=true` and review the diff.
 */
class TestPatternTest {
    private val page = TestPattern.render()

    @Test
    fun `page is head-width and a sensible length`() {
        assertEquals(384, page.width)
        assertEquals(48, page.bytesPerRow)
        assertTrue(page.height in 100..400, "height ${page.height}")
    }

    @Test
    fun `top band is solid black to both edges`() {
        for (y in 0 until 16) {
            assertTrue(page.isBlack(0, y) && page.isBlack(383, y) && page.isBlack(191, y), "row $y")
        }
        assertFalse(page.isBlack(0, 16))
    }

    @Test
    fun `bar band starts with single-dot bars`() {
        val y = 16 + 8 + 1
        assertTrue(page.isBlack(0, y))
        assertFalse(page.isBlack(1, y))
        assertTrue(page.isBlack(2, y))
    }

    @Test
    fun `ramp goes from white on the left to black on the right`() {
        val y = page.height - 8 - 1
        assertFalse(page.isBlack(0, y))
        assertTrue(page.isBlack(383, y))
    }

    @Test
    fun `matches the golden PBM`() = Pbm.assertMatchesGolden("test_pattern", page)
}

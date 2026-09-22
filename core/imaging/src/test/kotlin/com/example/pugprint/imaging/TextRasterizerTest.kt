package com.example.pugprint.imaging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class TextRasterizerTest {
    private val font = FontCatalog.Blocky

    @Test
    fun `a line is scaled glyphs with scaled spacing`() {
        val line = TextRasterizer.renderLine("HI", font, scale = 3)
        assertEquals((5 + 1 + 5) * 3, line.width)
        assertEquals(7 * 3, line.height)
        assertTrue(line[0, 0]) // H's left stem, top-left dot
        assertTrue(line[2, 2]) // still inside the 3x3 block
        assertTrue(!line[3, 0]) // first gap column of H
    }

    @Test
    fun `blank text renders nothing`() {
        assertNull(TextRasterizer.renderBlock("   ", font, maxWidth = 384))
    }

    @Test
    fun `short text takes the biggest scale`() {
        val block = TextRasterizer.renderBlock("HI", font, maxWidth = 360)!!
        assertEquals(7 * TextRasterizer.MAX_SCALE, block.height)
        assertEquals(font.measure("HI") * TextRasterizer.MAX_SCALE, block.width)
    }

    @Test
    fun `long text wraps and shrinks until it fits three lines`() {
        val block =
            TextRasterizer.renderBlock(
                "happy birthday to the best dog in the whole world",
                font,
                maxWidth = 360,
            )!!
        assertTrue(block.width <= 360, "wider than the band: ${block.width}")
        val tallestAllowed = (7 * TextRasterizer.MAX_LINES + font.spacing.line * (TextRasterizer.MAX_LINES - 1)) * 3
        assertTrue(block.height <= tallestAllowed, "too tall for 3 lines at scale 3: ${block.height}")
    }

    @Test
    fun `a single enormous word is chopped rather than dropped`() {
        val block = TextRasterizer.renderBlock("SUPERCALIFRAGILISTICEXPIALIDOCIOUS", font, maxWidth = 120)!!
        assertTrue(block.width <= 120, "wider than the band: ${block.width}")
        assertTrue(block.height >= 7 * TextRasterizer.MIN_SCALE)
    }

    @TestFactory
    fun `every font matches its golden sample`(): List<DynamicTest> =
        FontCatalog.all.map { font ->
            DynamicTest.dynamicTest(font.id) {
                val sample =
                    TextRasterizer.renderBlock(
                        "The quick brown fox jumps over the lazy dog 0123456789 !?.,'-",
                        font,
                        maxWidth = 384,
                        maxScale = 2,
                    )!!
                Pbm.assertMatchesGolden("font_${font.id}", sample.toMonoBitmap())
            }
        }
}

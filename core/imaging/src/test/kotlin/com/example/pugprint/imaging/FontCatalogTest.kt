package com.example.pugprint.imaging

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/** The rules every font in [FontCatalog.all] must follow; a new font is checked automatically. */
class FontCatalogTest {
    @Test
    fun `ids are unique, lowercase and never blank`() {
        val ids = FontCatalog.all.map { it.id }
        assertEquals(ids.toSet().size, ids.size, "duplicate font ids in $ids")
        ids.forEach { assertTrue(it.isNotBlank() && it == it.lowercase() && ' ' !in it, "bad font id '$it'") }
    }

    @Test
    fun `the default is in the catalog and unknown ids fall back to it`() {
        assertTrue(FontCatalog.default in FontCatalog.all)
        assertSame(FontCatalog.default, FontCatalog.byId("no-such-font"))
        FontCatalog.all.forEach { assertSame(it, FontCatalog.byId(it.id)) }
    }

    @TestFactory
    fun `every font can spell letters, digits and basic punctuation`(): List<DynamicTest> =
        FontCatalog.all.map { font ->
            DynamicTest.dynamicTest(font.id) {
                val needed = ('A'..'Z') + ('0'..'9') + listOf(' ', '.', ',', '!', '?', '\'', '-')
                val missing = needed.filterNot(font::has)
                assertTrue(missing.isEmpty(), "${font.id} is missing $missing")
            }
        }

    @Test
    fun `lower case uses the upper case glyph and unknown characters use the fallback`() {
        val font = FontCatalog.Blocky
        assertSame(font.glyph('A'), font.glyph('a'))
        assertSame(font.fallback, font.glyph('Ω'))
    }

    @Test
    fun `measure adds letter spacing between glyphs only`() {
        val font = FontCatalog.Blocky
        assertEquals(0, font.measure(""))
        assertEquals(5, font.measure("A"))
        assertEquals(5 + 1 + 5, font.measure("AB"))
    }
}

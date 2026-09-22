package com.example.pugprint.design

import androidx.compose.ui.graphics.Color
import com.example.pugprint.design.theme.Contrast
import com.example.pugprint.design.theme.PugPalette
import com.example.pugprint.design.theme.ThemeCatalog
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

/**
 * The rules every theme in [ThemeCatalog.all] must follow. Adding a theme adds it to these
 * checks automatically; a failure names the theme and the colour pair to fix.
 */
class ThemeCatalogTest {
    @Test
    fun `ids are unique, lowercase and never blank`() {
        val ids = ThemeCatalog.all.map { it.id }
        assertEquals(ids.toSet().size, ids.size, "duplicate theme ids in $ids")
        ids.forEach { id ->
            assertTrue(id.isNotBlank() && id == id.lowercase() && ' ' !in id, "bad theme id '$id'")
        }
    }

    @Test
    fun `every theme has a name and an emoji`() {
        ThemeCatalog.all.forEach { theme ->
            assertTrue(theme.displayName.isNotBlank(), "${theme.id} has no display name")
            assertTrue(theme.emoji.isNotBlank(), "${theme.id} has no emoji")
        }
    }

    @Test
    fun `the default is in the catalog and unknown ids fall back to it`() {
        assertTrue(ThemeCatalog.default in ThemeCatalog.all)
        assertSame(ThemeCatalog.default, ThemeCatalog.byId("no-such-theme"))
        assertSame(ThemeCatalog.default, ThemeCatalog.byId(null))
        ThemeCatalog.all.forEach { assertSame(it, ThemeCatalog.byId(it.id)) }
    }

    @Test
    fun `all colours are opaque`() {
        ThemeCatalog.all.forEach { theme ->
            theme.palette.readablePairs().flatMap { listOf(it.foreground, it.background) }.forEach { colour ->
                assertEquals(1f, colour.alpha, "${theme.id}: $colour is not opaque")
            }
        }
    }

    @TestFactory
    fun `text stays readable on every background (WCAG AA)`(): List<DynamicTest> =
        ThemeCatalog.all.flatMap { theme ->
            theme.palette.readablePairs().map { pair ->
                DynamicTest.dynamicTest("${theme.id}: ${pair.name}") {
                    val ratio = Contrast.ratio(pair.foreground, pair.background)
                    assertTrue(
                        ratio >= pair.minimum,
                        "${theme.id}: ${pair.name} is ${"%.2f".format(ratio)}:1, needs ${pair.minimum}:1",
                    )
                }
            }
        }

    private data class ReadablePair(
        val name: String,
        val foreground: Color,
        val background: Color,
        val minimum: Double,
    )

    /** Every foreground/background combination the kit actually draws. */
    private fun PugPalette.readablePairs() =
        listOf(
            ReadablePair("text on background", text, background, Contrast.AA_TEXT),
            ReadablePair("text on card", text, card, Contrast.AA_TEXT),
            ReadablePair("text on primarySoft", text, primarySoft, Contrast.AA_TEXT),
            ReadablePair("text on errorSoft", text, errorSoft, Contrast.AA_TEXT),
            ReadablePair("textSoft on background", textSoft, background, Contrast.AA_TEXT),
            ReadablePair("textSoft on card", textSoft, card, Contrast.AA_TEXT),
            ReadablePair("onPrimary on primary", onPrimary, primary, Contrast.AA_TEXT),
            ReadablePair("onSecondary on secondary", onSecondary, secondary, Contrast.AA_TEXT),
            ReadablePair("error on errorSoft", error, errorSoft, Contrast.AA_TEXT),
            ReadablePair("primary on background", primary, background, Contrast.AA_TEXT),
            ReadablePair("outline on background", outline, background, Contrast.AA_LARGE),
        )
}

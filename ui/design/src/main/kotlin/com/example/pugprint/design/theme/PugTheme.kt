package com.example.pugprint.design.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The colours of one look. Every field is a plain colour a designer can change; the kit maps
 * them onto Material so screens never pick colours themselves.
 *
 * Pairs that must stay readable (checked by `ThemeCatalogTest`): [text] on [background],
 * [text] on [card], [text] on [primarySoft], [text] on [errorSoft], [onPrimary] on [primary],
 * [onSecondary] on [secondary].
 */
@Immutable
data class PugPalette(
    /** Behind everything. */
    val background: Color,
    /** Cards, banners and unselected choices. */
    val card: Color,
    /** Body text and titles. */
    val text: Color,
    /** Hints and less important text. */
    val textSoft: Color,
    /** The big buttons and the selected choice. */
    val primary: Color,
    /** Text on [primary]. */
    val onPrimary: Color,
    /** A lighter [primary] for selected tiles and "done" banners. */
    val primarySoft: Color,
    /** Second-choice buttons. */
    val secondary: Color,
    /** Text on [secondary]. */
    val onSecondary: Color,
    /** Thin lines around cards and previews. */
    val outline: Color,
    /** "Something is wrong" text and icons. */
    val error: Color,
    /** Behind a "something is wrong" banner. */
    val errorSoft: Color,
)

/**
 * One entry in the [ThemeCatalog]: a name, a palette and how round the corners are.
 *
 * @property id stable key saved in settings; never rename an id once shipped.
 * @property displayName what the theme picker shows.
 * @property roundness corner radius of buttons, tiles and banners.
 * @property fontFamily the typeface; [FontFamily.Default] unless a bundled font is added.
 */
@Immutable
data class PugTheme(
    val id: String,
    val displayName: String,
    val palette: PugPalette,
    val roundness: Dp = 20.dp,
    val fontFamily: FontFamily = FontFamily.Default,
)

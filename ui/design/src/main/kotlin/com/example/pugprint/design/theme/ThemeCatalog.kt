package com.example.pugprint.design.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Every look the app can wear. **To add a theme, add one [PugTheme] to [all]** — the theme
 * picker, the readability test and the gallery goldens all read this list.
 *
 * Rules: ids are unique and never change once shipped; readability is checked by
 * `ThemeCatalogTest` (WCAG AA contrast for every text/background pair in [PugPalette]).
 */
object ThemeCatalog {
    /** The warm brown-and-honey look PugPrint started with. */
    val Pug =
        PugTheme(
            id = "pug",
            displayName = "Pug",
            emoji = "🐶",
            palette =
                PugPalette(
                    background = Color(0xFFFFF8EE),
                    card = Color(0xFFFFEBD2),
                    text = Color(0xFF3A2E2A),
                    textSoft = Color(0xFF6B5A52),
                    primary = Color(0xFF8C5A2B),
                    onPrimary = Color(0xFFFFFFFF),
                    primarySoft = Color(0xFFF5D9B8),
                    secondary = Color(0xFFE0A458),
                    onSecondary = Color(0xFF3A2E2A),
                    outline = Color(0xFF9A7455),
                    error = Color(0xFFB3261E),
                    errorSoft = Color(0xFFF9DEDC),
                ),
            roundness = 20.dp,
        )

    /** Pink and purple, extra round. */
    val Bubblegum =
        PugTheme(
            id = "bubblegum",
            displayName = "Bubblegum",
            emoji = "🍬",
            palette =
                PugPalette(
                    background = Color(0xFFFFF0F6),
                    card = Color(0xFFFFD9E8),
                    text = Color(0xFF3B1F2E),
                    textSoft = Color(0xFF6E4A5C),
                    primary = Color(0xFFC2185B),
                    onPrimary = Color(0xFFFFFFFF),
                    primarySoft = Color(0xFFFFC1DA),
                    secondary = Color(0xFF7E57C2),
                    onSecondary = Color(0xFFFFFFFF),
                    outline = Color(0xFFBE5A88),
                    error = Color(0xFFB3261E),
                    errorSoft = Color(0xFFF9DEDC),
                ),
            roundness = 28.dp,
        )

    /** Teal and sea-blue, squarer corners. */
    val Ocean =
        PugTheme(
            id = "ocean",
            displayName = "Ocean",
            emoji = "🐳",
            palette =
                PugPalette(
                    background = Color(0xFFEAF7FA),
                    card = Color(0xFFCFEFF5),
                    text = Color(0xFF10303A),
                    textSoft = Color(0xFF3F6470),
                    primary = Color(0xFF00695C),
                    onPrimary = Color(0xFFFFFFFF),
                    primarySoft = Color(0xFFA7E8DF),
                    secondary = Color(0xFF4DD0E1),
                    onSecondary = Color(0xFF10303A),
                    outline = Color(0xFF3E8FA0),
                    error = Color(0xFFB3261E),
                    errorSoft = Color(0xFFF9DEDC),
                ),
            roundness = 12.dp,
        )

    /** In picker order. */
    val all: List<PugTheme> = listOf(Pug, Bubblegum, Ocean)

    val default: PugTheme = Pug

    /** The theme saved under [id], or [default] if the id is unknown (e.g. a theme was removed). */
    fun byId(id: String?): PugTheme = all.firstOrNull { it.id == id } ?: default
}

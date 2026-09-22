package com.example.pugprint.imaging

/** One letter as a little grid of dots, drawn in the catalog as rows of `#` (black) and `.`. */
public class Glyph(
    public val width: Int,
    public val height: Int,
    private val dots: BooleanArray,
) {
    public operator fun get(
        x: Int,
        y: Int,
    ): Boolean = dots[y * width + x]

    public companion object {
        /** Parses rows like `".###."`; every row must be the same width. */
        public fun of(vararg rows: String): Glyph {
            require(rows.isNotEmpty()) { "A glyph needs at least one row" }
            val width = rows[0].length
            require(rows.all { it.length == width }) { "Glyph rows differ in width: ${rows.toList()}" }
            val dots = BooleanArray(width * rows.size)
            rows.forEachIndexed { y, row -> row.forEachIndexed { x, c -> dots[y * width + x] = c == '#' } }
            return Glyph(width, rows.size, dots)
        }
    }
}

/** Gaps in a [PixelFont], in unscaled dots. */
public data class FontSpacing(
    /** Between letters. */
    val letter: Int = 1,
    /** Between lines. */
    val line: Int = 2,
)

/**
 * A bitmap typeface: fixed [glyphHeight], glyphs of any width, drawn at integer scales so the
 * dots stay crisp on the print head. Letters are upper-case only; lower-case input is upper-cased
 * and anything without a glyph is drawn as [fallback].
 *
 * @property id stable key saved with a sticker; never rename once shipped.
 */
public class PixelFont(
    public val id: String,
    public val displayName: String,
    public val glyphHeight: Int,
    private val glyphs: Map<Char, Glyph>,
    public val fallback: Glyph,
    public val spacing: FontSpacing = FontSpacing(),
) {
    init {
        require(glyphs.values.all { it.height == glyphHeight }) { "$id: every glyph must be $glyphHeight rows" }
        require(fallback.height == glyphHeight) { "$id: fallback must be $glyphHeight rows" }
        require(' ' in glyphs) { "$id: needs a space glyph" }
    }

    /** The glyph for [char] (upper-cased), or [fallback]. */
    public fun glyph(char: Char): Glyph = glyphs[char.uppercaseChar()] ?: glyphs[char] ?: fallback

    public fun has(char: Char): Boolean = char.uppercaseChar() in glyphs || char in glyphs

    /** The characters this font can draw. */
    public val characters: Set<Char> get() = glyphs.keys

    /** Width in unscaled dots of [text] on one line, including letter spacing. */
    public fun measure(text: String): Int {
        if (text.isEmpty()) return 0
        return text.sumOf { glyph(it).width } + (text.length - 1) * spacing.letter
    }
}

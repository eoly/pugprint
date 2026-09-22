package com.example.pugprint.imaging

/** Draws words with a [PixelFont]: one line at a fixed scale, or a wrapped, auto-sized block. */
public object TextRasterizer {
    /** Largest scale tried for a caption; 6 × 7 rows ≈ 5 mm tall letters on the print head. */
    public const val MAX_SCALE: Int = 6

    /** Smallest readable scale on thermal paper. */
    public const val MIN_SCALE: Int = 2

    /** A caption never takes more lines than this. */
    public const val MAX_LINES: Int = 3

    /** [text] on one line, each font dot drawn as a [scale] × [scale] block. */
    public fun renderLine(
        text: String,
        font: PixelFont,
        scale: Int,
    ): BitCanvas {
        require(scale >= 1) { "scale must be at least 1" }
        val width = font.measure(text).coerceAtLeast(1) * scale
        val canvas = BitCanvas(width, font.glyphHeight * scale)
        var pen = 0
        text.forEach { char ->
            val glyph = font.glyph(char)
            drawGlyph(canvas, glyph, pen * scale, scale)
            pen += glyph.width + font.spacing.letter
        }
        return canvas
    }

    private fun drawGlyph(
        canvas: BitCanvas,
        glyph: Glyph,
        left: Int,
        scale: Int,
    ) {
        for (gy in 0 until glyph.height) {
            for (gx in 0 until glyph.width) {
                if (glyph[gx, gy]) canvas.fillRect(left + gx * scale, gy * scale, scale, scale, isBlack = true)
            }
        }
    }

    /**
     * [text] wrapped to at most [maxLines] lines and centred, at the largest scale ≤ [maxScale]
     * where it fits [maxWidth]; falls back to [MIN_SCALE] and, if a single word is still too
     * wide, breaks the word. Blank text gives `null`.
     */
    public fun renderBlock(
        text: String,
        font: PixelFont,
        maxWidth: Int,
        maxLines: Int = MAX_LINES,
        maxScale: Int = MAX_SCALE,
    ): BitCanvas? {
        val words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return null
        val layout =
            (maxScale downTo MIN_SCALE).firstNotNullOfOrNull { scale ->
                wrap(words, font, maxWidth / scale)?.takeIf { it.size <= maxLines }?.let { it to scale }
            } ?: (forceWrap(words, font, maxWidth / MIN_SCALE).take(maxLines) to MIN_SCALE)
        val (lines, scale) = layout
        val rendered = lines.map { renderLine(it, font, scale) }
        val gap = font.spacing.line * scale
        val height = rendered.sumOf { it.height } + gap * (rendered.size - 1)
        val block = BitCanvas(rendered.maxOf { it.width }, height)
        var y = 0
        rendered.forEach { line ->
            block.drawBlack(line, (block.width - line.width) / 2, y)
            y += line.height + gap
        }
        return block
    }

    /** Greedy word wrap in unscaled dots; `null` if any single word is wider than [maxDots]. */
    private fun wrap(
        words: List<String>,
        font: PixelFont,
        maxDots: Int,
    ): List<String>? {
        if (words.any { font.measure(it) > maxDots }) return null
        val lines = ArrayList<String>()
        var current = ""
        words.forEach { word ->
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (font.measure(candidate) <= maxDots) {
                current = candidate
            } else {
                lines += current
                current = word
            }
        }
        lines += current
        return lines
    }

    /** Like [wrap] but chops words that cannot fit, so something always renders. */
    private fun forceWrap(
        words: List<String>,
        font: PixelFont,
        maxDots: Int,
    ): List<String> {
        val pieces = ArrayList<String>()
        words.forEach { word ->
            var rest = word
            while (font.measure(rest) > maxDots && rest.length > 1) {
                var cut = rest.length - 1
                while (cut > 1 && font.measure(rest.substring(0, cut)) > maxDots) cut--
                pieces += rest.substring(0, cut)
                rest = rest.substring(cut)
            }
            pieces += rest
        }
        return wrap(pieces, font, maxDots) ?: pieces
    }
}

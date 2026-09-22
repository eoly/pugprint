package com.example.pugprint.imaging

private const val SMALL_SCALE = 4
private const val MEDIUM_SCALE = 6
private const val BIG_SCALE = 8

/** How big a stamp prints: print dots per art dot. */
public enum class StampSize(
    public val scale: Int,
) {
    /** 16-dot art → 64 dots ≈ 8 mm. */
    SMALL(SMALL_SCALE),

    /** ≈ 12 mm. */
    MEDIUM(MEDIUM_SCALE),

    /** ≈ 16 mm. */
    BIG(BIG_SCALE),
}

/**
 * Draws a [Stamp] at a [StampSize]: black art dots with a one-art-dot white halo so the
 * stamp reads over a dithered photo.
 */
public object StampRasterizer {
    /** Halo thickness in art dots. */
    public const val HALO: Int = 1

    /** The stamp alone, black on white, as the picker shows it. */
    public fun render(
        stamp: Stamp,
        size: StampSize,
    ): MonoBitmap {
        val art = stamp.art
        val canvas = BitCanvas(art.width * size.scale, art.height * size.scale)
        for (y in 0 until art.height) {
            for (x in 0 until art.width) {
                if (art[x, y]) canvas.fillRect(x * size.scale, y * size.scale, size.scale, size.scale, isBlack = true)
            }
        }
        return canvas.toMonoBitmap()
    }

    /** Paints [stamp] onto [canvas] centred at ([centerX], [centerY]) dots, halo first, clipped at the edges. */
    public fun draw(
        canvas: BitCanvas,
        stamp: Stamp,
        size: StampSize,
        centerX: Int,
        centerY: Int,
    ) {
        val art = stamp.art
        val scale = size.scale
        val left = centerX - art.width * scale / 2
        val top = centerY - art.height * scale / 2
        for (y in -HALO until art.height + HALO) {
            for (x in -HALO until art.width + HALO) {
                if (art.nearDot(x, y)) canvas.fillRect(left + x * scale, top + y * scale, scale, scale, isBlack = false)
            }
        }
        for (y in 0 until art.height) {
            for (x in 0 until art.width) {
                if (art[x, y]) canvas.fillRect(left + x * scale, top + y * scale, scale, scale, isBlack = true)
            }
        }
    }

    /** True when ([x], [y]) is a dot or touches one (8 neighbours), for the halo. */
    private fun Glyph.nearDot(
        x: Int,
        y: Int,
    ): Boolean {
        for (dy in -HALO..HALO) {
            for (dx in -HALO..HALO) {
                val nx = x + dx
                val ny = y + dy
                if (nx in 0 until width && ny in 0 until height && this[nx, ny]) return true
            }
        }
        return false
    }
}

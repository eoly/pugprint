package com.example.pugprint.imaging

/** A mutable 1-bit picture (`true` = black) that overlays are drawn onto before packing. */
public class BitCanvas(
    public val width: Int,
    public val height: Int,
    private val black: BooleanArray = BooleanArray(width * height),
) {
    init {
        require(width > 0 && height > 0) { "Dimensions must be positive: ${width}x$height" }
        require(black.size == width * height) { "Expected ${width * height} pixels, got ${black.size}" }
    }

    public operator fun get(
        x: Int,
        y: Int,
    ): Boolean = black[y * width + x]

    public operator fun set(
        x: Int,
        y: Int,
        isBlack: Boolean,
    ) {
        black[y * width + x] = isBlack
    }

    /** Paints a rectangle, clipped to the canvas. */
    public fun fillRect(
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        isBlack: Boolean,
    ) {
        for (py in maxOf(y, 0) until minOf(y + h, height)) {
            for (px in maxOf(x, 0) until minOf(x + w, width)) {
                black[py * width + px] = isBlack
            }
        }
    }

    /** Draws the black pixels of [source] with its top-left at ([x], [y]), clipped; white pixels are left alone. */
    public fun drawBlack(
        source: BitCanvas,
        x: Int,
        y: Int,
    ) {
        for (sy in 0 until source.height) {
            val py = y + sy
            if (py !in 0 until height) continue
            for (sx in 0 until source.width) {
                val px = x + sx
                if (px in 0 until width && source[sx, sy]) black[py * width + px] = true
            }
        }
    }

    /**
     * Paints white everything outside the ellipse inscribed in the canvas (a circle when it is
     * square), so a round sticker gets no ink on its backing. A dot is kept when its centre is inside.
     */
    public fun clearOutsideEllipse() {
        val rx = width / 2.0
        val ry = height / 2.0
        for (y in 0 until height) {
            val dy = (y + HALF - ry) / ry
            for (x in 0 until width) {
                val dx = (x + HALF - rx) / rx
                if (dx * dx + dy * dy > 1.0) black[y * width + x] = false
            }
        }
    }

    public fun toMonoBitmap(): MonoBitmap = MonoBitmap.fromPixels(width, height, black)

    public companion object {
        private const val HALF = 0.5

        public fun from(bitmap: MonoBitmap): BitCanvas {
            val canvas = BitCanvas(bitmap.width, bitmap.height)
            for (y in 0 until bitmap.height) {
                for (x in 0 until bitmap.width) canvas[x, y] = bitmap.isBlack(x, y)
            }
            return canvas
        }
    }
}

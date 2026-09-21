package com.example.pugprint.imaging

/**
 * A built-in test page for a 384-dot head, used by the "print test page" flow before the
 * image pipeline exists (Phase 4). Each band checks one thing on paper:
 *
 * 1. solid black — density / both paper edges reachable
 * 2. vertical bars of 1, 2, 4, 8 and 16 dots — horizontal resolution
 * 3. horizontal rules of 1, 2 and 4 rows — vertical resolution and row pacing
 * 4. 8-dot checkerboard — alignment across rows
 * 5. Bayer-dithered ramp — greys survive the dither the way a photo will
 */
public object TestPattern {
    public const val WIDTH: Int = 384

    private const val BLACK_BAND_ROWS = 16
    private const val GAP_ROWS = 8
    private const val BAR_BAND_ROWS = 24
    private const val CHECKER_ROWS = 32
    private const val CHECKER_CELL = 8
    private const val RAMP_ROWS = 32
    private val BAR_WIDTHS = intArrayOf(1, 2, 4, 8, 16)
    private val RULE_HEIGHTS = intArrayOf(1, 2, 4)
    private const val RULE_GAP_ROWS = 3
    private const val BAYER_SIZE = 4
    private val BAYER =
        arrayOf(
            intArrayOf(0, 8, 2, 10),
            intArrayOf(12, 4, 14, 6),
            intArrayOf(3, 11, 1, 9),
            intArrayOf(15, 7, 13, 5),
        )

    /** Renders the page; the result is deterministic and covered by a golden PBM test. */
    public fun render(): MonoBitmap {
        val rows = ArrayList<BooleanArray>()
        repeat(BLACK_BAND_ROWS) { rows += BooleanArray(WIDTH) { true } }
        gap(rows)
        val bars = barRow()
        repeat(BAR_BAND_ROWS) { rows += bars }
        gap(rows)
        RULE_HEIGHTS.forEachIndexed { index, height ->
            if (index > 0) repeat(RULE_GAP_ROWS) { rows += BooleanArray(WIDTH) }
            repeat(height) { rows += BooleanArray(WIDTH) { true } }
        }
        gap(rows)
        repeat(CHECKER_ROWS) { y ->
            rows +=
                BooleanArray(WIDTH) { x -> ((x / CHECKER_CELL) + (y / CHECKER_CELL)) % 2 == 0 }
        }
        gap(rows)
        repeat(RAMP_ROWS) { y -> rows += rampRow(y) }
        gap(rows)
        return MonoBitmap.fromPixels(WIDTH, rows.size, BooleanArray(WIDTH * rows.size) { rows[it / WIDTH][it % WIDTH] })
    }

    private fun gap(rows: MutableList<BooleanArray>) {
        repeat(GAP_ROWS) { rows += BooleanArray(WIDTH) }
    }

    /** Groups of bars, each group `n` dots on / `n` dots off, one group per entry of [BAR_WIDTHS]. */
    private fun barRow(): BooleanArray {
        val row = BooleanArray(WIDTH)
        val groupWidth = WIDTH / BAR_WIDTHS.size
        for ((group, width) in BAR_WIDTHS.withIndex()) {
            val start = group * groupWidth
            for (x in start until start + groupWidth) {
                row[x] = ((x - start) / width) % 2 == 0
            }
        }
        return row
    }

    /** Left = white, right = black, thresholded through a 4×4 Bayer matrix. */
    private fun rampRow(y: Int): BooleanArray =
        BooleanArray(WIDTH) { x ->
            val level = x * (BAYER_SIZE * BAYER_SIZE) / WIDTH
            level > BAYER[y % BAYER_SIZE][x % BAYER_SIZE]
        }
}

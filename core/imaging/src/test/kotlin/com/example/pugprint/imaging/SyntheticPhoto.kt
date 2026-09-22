package com.example.pugprint.imaging

import kotlin.math.hypot

/** A deterministic stand-in for a photo: a diagonal ramp, a soft dark disc and a hard-edged stripe block. */
object SyntheticPhoto {
    const val WIDTH = 128
    const val HEIGHT = 96

    fun render(): GrayImage =
        GrayImage.generate(WIDTH, HEIGHT) { x, y ->
            val ramp = (x + y) * 255 / (WIDTH + HEIGHT - 2)
            val distance = hypot(x - 40.0, y - 48.0)
            val disc = if (distance < 24) (distance * 8).toInt() else 255
            val stripes = if (x in 88 until 120 && y in 16 until 80) (if ((x / 4) % 2 == 0) 0 else 255) else 255
            minOf(ramp.coerceAtLeast(32), disc, stripes)
        }
}

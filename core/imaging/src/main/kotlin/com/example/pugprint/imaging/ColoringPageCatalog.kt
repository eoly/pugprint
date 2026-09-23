package com.example.pugprint.imaging

import com.example.pugprint.imaging.BrushSize.MEDIUM
import com.example.pugprint.imaging.BrushSize.THIN

/**
 * Every coloring page in the app. **To add a page, add one [ColoringPage] to [all]** and draw it
 * with [Outline]: positions are fractions of the page (0.0 = left/top, 1.0 = right/bottom),
 * [MEDIUM] lines for the outline a crayon stays inside, [THIN] for the details. Keep all the ink
 * within [ColoringPage.SAFE_RADIUS] of the centre so the page prints whole on round stickers too.
 * `ColoringPageCatalogTest` checks the rules; the picker and the goldens read this list.
 */
public object ColoringPageCatalog {
    public val Pug: ColoringPage =
        ColoringPage(
            id = "pug",
            displayName = "Pug",
            drawing =
                Outline.build {
                    circle(cx = 0.5f, cy = 0.53f, r = 0.30f) // head
                    path(MEDIUM, 0.28f to 0.30f, 0.17f to 0.36f, 0.14f to 0.55f, 0.22f to 0.62f) // left ear
                    path(MEDIUM, 0.72f to 0.30f, 0.83f to 0.36f, 0.86f to 0.55f, 0.78f to 0.62f) // right ear
                    path( // wrinkle
                        THIN,
                        0.42f to 0.36f,
                        0.46f to 0.33f,
                        0.50f to 0.36f,
                        0.54f to 0.33f,
                        0.58f to 0.36f,
                    )
                    circle(cx = 0.40f, cy = 0.45f, r = 0.055f, size = THIN) // eyes
                    circle(cx = 0.60f, cy = 0.45f, r = 0.055f, size = THIN)
                    dot(x = 0.40f, y = 0.45f)
                    dot(x = 0.60f, y = 0.45f)
                    ellipse(cx = 0.5f, cy = 0.62f, rx = 0.17f, ry = 0.12f, size = THIN) // muzzle
                    loop(THIN, 0.45f to 0.57f, 0.55f to 0.57f, 0.50f to 0.63f) // nose
                    line(x0 = 0.50f, y0 = 0.63f, x1 = 0.50f, y1 = 0.68f, size = THIN)
                    path(THIN, 0.42f to 0.66f, 0.46f to 0.70f, 0.50f to 0.68f, 0.54f to 0.70f, 0.58f to 0.66f) // mouth
                },
        )

    public val Star: ColoringPage =
        ColoringPage(
            id = "star",
            displayName = "Star",
            drawing =
                Outline.build {
                    star(cx = 0.5f, cy = 0.52f, outer = 0.40f, inner = 0.17f)
                    dot(x = 0.45f, y = 0.48f)
                    dot(x = 0.55f, y = 0.48f)
                    arc(cx = 0.5f, cy = 0.54f, rx = 0.06f, ry = 0.04f, fromDegrees = 20f, toDegrees = 160f, size = THIN)
                },
        )

    public val Flower: ColoringPage =
        ColoringPage(
            id = "flower",
            displayName = "Flower",
            drawing =
                Outline.build {
                    for (petal in 0 until PETALS) {
                        val a = Math.toRadians(petal * FULL_TURN / PETALS)
                        circle(
                            cx = 0.5f + (PETAL_REACH * Math.cos(a)).toFloat(),
                            cy = 0.5f + (PETAL_REACH * Math.sin(a)).toFloat(),
                            r = 0.12f,
                        )
                    }
                    circle(cx = 0.5f, cy = 0.5f, r = 0.11f)
                    dot(x = 0.46f, y = 0.47f)
                    dot(x = 0.54f, y = 0.47f)
                    arc(
                        cx = 0.5f,
                        cy = 0.51f,
                        rx = 0.045f,
                        ry = 0.03f,
                        fromDegrees = 20f,
                        toDegrees = 160f,
                        size = THIN,
                    )
                },
        )

    public val Fish: ColoringPage =
        ColoringPage(
            id = "fish",
            displayName = "Fish",
            drawing =
                Outline.build {
                    ellipse(cx = 0.45f, cy = 0.5f, rx = 0.27f, ry = 0.17f) // body
                    loop(MEDIUM, 0.70f to 0.50f, 0.85f to 0.36f, 0.83f to 0.50f, 0.85f to 0.64f) // tail
                    path(MEDIUM, 0.40f to 0.34f, 0.47f to 0.22f, 0.58f to 0.35f) // top fin
                    path(MEDIUM, 0.42f to 0.66f, 0.47f to 0.76f, 0.55f to 0.66f) // belly fin
                    circle(cx = 0.28f, cy = 0.46f, r = 0.035f, size = THIN) // eye
                    dot(x = 0.29f, y = 0.46f, size = THIN)
                    arc( // gill
                        cx = 0.40f,
                        cy = 0.5f,
                        rx = 0.06f,
                        ry = 0.13f,
                        fromDegrees = 110f,
                        toDegrees = 250f,
                        size = THIN,
                    )
                    arc(
                        cx = 0.52f,
                        cy = 0.42f,
                        rx = 0.05f,
                        ry = 0.05f,
                        fromDegrees = 300f,
                        toDegrees = 420f,
                        size = THIN,
                    )
                    arc(
                        cx = 0.56f,
                        cy = 0.52f,
                        rx = 0.05f,
                        ry = 0.05f,
                        fromDegrees = 300f,
                        toDegrees = 420f,
                        size = THIN,
                    )
                    arc(
                        cx = 0.50f,
                        cy = 0.60f,
                        rx = 0.05f,
                        ry = 0.05f,
                        fromDegrees = 300f,
                        toDegrees = 420f,
                        size = THIN,
                    )
                },
        )

    public val Butterfly: ColoringPage =
        ColoringPage(
            id = "butterfly",
            displayName = "Butterfly",
            drawing =
                Outline.build {
                    ellipse(cx = 0.31f, cy = 0.44f, rx = 0.18f, ry = 0.15f) // upper wings
                    ellipse(cx = 0.69f, cy = 0.44f, rx = 0.18f, ry = 0.15f)
                    ellipse(cx = 0.35f, cy = 0.66f, rx = 0.14f, ry = 0.12f) // lower wings
                    ellipse(cx = 0.65f, cy = 0.66f, rx = 0.14f, ry = 0.12f)
                    ellipse(cx = 0.5f, cy = 0.55f, rx = 0.045f, ry = 0.20f) // body
                    circle(cx = 0.5f, cy = 0.31f, r = 0.05f) // head
                    line(x0 = 0.48f, y0 = 0.27f, x1 = 0.41f, y1 = 0.16f, size = THIN) // antennae
                    line(x0 = 0.52f, y0 = 0.27f, x1 = 0.59f, y1 = 0.16f, size = THIN)
                    dot(x = 0.41f, y = 0.16f)
                    dot(x = 0.59f, y = 0.16f)
                    circle(cx = 0.29f, cy = 0.43f, r = 0.05f, size = THIN) // spots
                    circle(cx = 0.71f, cy = 0.43f, r = 0.05f, size = THIN)
                    circle(cx = 0.34f, cy = 0.67f, r = 0.035f, size = THIN)
                    circle(cx = 0.66f, cy = 0.67f, r = 0.035f, size = THIN)
                },
        )

    public val Rocket: ColoringPage =
        ColoringPage(
            id = "rocket",
            displayName = "Rocket",
            drawing =
                Outline.build {
                    loop(MEDIUM, 0.50f to 0.13f, 0.63f to 0.32f, 0.63f to 0.68f, 0.37f to 0.68f, 0.37f to 0.32f) // body
                    loop(MEDIUM, 0.37f to 0.50f, 0.25f to 0.72f, 0.37f to 0.68f) // fins
                    loop(MEDIUM, 0.63f to 0.50f, 0.75f to 0.72f, 0.63f to 0.68f)
                    line(x0 = 0.37f, y0 = 0.32f, x1 = 0.63f, y1 = 0.32f, size = THIN) // nose cone edge
                    circle(cx = 0.5f, cy = 0.42f, r = 0.07f, size = THIN) // window
                    path( // flame
                        MEDIUM,
                        0.42f to 0.68f,
                        0.46f to 0.82f,
                        0.50f to 0.73f,
                        0.54f to 0.82f,
                        0.58f to 0.68f,
                    )
                },
        )

    public val Heart: ColoringPage =
        ColoringPage(
            id = "heart",
            displayName = "Heart",
            drawing =
                Outline.build {
                    // Each lobe is a circle; the arcs run from where the straight sides touch them up over
                    // the top to the dip in the middle.
                    arc(cx = 0.33f, cy = 0.40f, rx = 0.17f, ry = 0.17f, fromDegrees = 138.6f, toDegrees = 360f)
                    arc(cx = 0.67f, cy = 0.40f, rx = 0.17f, ry = 0.17f, fromDegrees = 180f, toDegrees = 401.4f)
                    line(x0 = 0.2025f, y0 = 0.512f, x1 = 0.5f, y1 = 0.85f)
                    line(x0 = 0.7975f, y0 = 0.512f, x1 = 0.5f, y1 = 0.85f)
                    dot(x = 0.42f, y = 0.48f)
                    dot(x = 0.58f, y = 0.48f)
                    arc(
                        cx = 0.5f,
                        cy = 0.55f,
                        rx = 0.07f,
                        ry = 0.045f,
                        fromDegrees = 20f,
                        toDegrees = 160f,
                        size = THIN,
                    )
                },
        )

    public val IceCream: ColoringPage =
        ColoringPage(
            id = "ice-cream",
            displayName = "Ice cream",
            drawing =
                Outline.build {
                    loop(MEDIUM, 0.31f to 0.46f, 0.69f to 0.46f, 0.50f to 0.88f) // cone
                    arc(cx = 0.5f, cy = 0.38f, rx = 0.20f, ry = 0.20f, fromDegrees = 155f, toDegrees = 385f) // scoop
                    line(x0 = 0.40f, y0 = 0.50f, x1 = 0.56f, y1 = 0.70f, size = THIN) // waffle
                    line(x0 = 0.60f, y0 = 0.50f, x1 = 0.44f, y1 = 0.70f, size = THIN)
                    line(x0 = 0.37f, y0 = 0.58f, x1 = 0.50f, y1 = 0.76f, size = THIN)
                    line(x0 = 0.63f, y0 = 0.58f, x1 = 0.50f, y1 = 0.76f, size = THIN)
                    circle(cx = 0.5f, cy = 0.155f, r = 0.04f, size = THIN) // cherry
                    line(x0 = 0.50f, y0 = 0.12f, x1 = 0.53f, y1 = 0.08f, size = THIN)
                    line(x0 = 0.42f, y0 = 0.30f, x1 = 0.45f, y1 = 0.27f, size = THIN) // sprinkles
                    line(x0 = 0.56f, y0 = 0.26f, x1 = 0.59f, y1 = 0.29f, size = THIN)
                    line(x0 = 0.38f, y0 = 0.40f, x1 = 0.41f, y1 = 0.38f, size = THIN)
                    line(x0 = 0.60f, y0 = 0.38f, x1 = 0.63f, y1 = 0.41f, size = THIN)
                },
        )

    public val Cat: ColoringPage =
        ColoringPage(
            id = "cat",
            displayName = "Cat",
            drawing =
                Outline.build {
                    circle(cx = 0.5f, cy = 0.55f, r = 0.28f) // head
                    path(MEDIUM, 0.302f to 0.352f, 0.26f to 0.18f, 0.451f to 0.274f) // ears
                    path(MEDIUM, 0.698f to 0.352f, 0.74f to 0.18f, 0.549f to 0.274f)
                    circle(cx = 0.40f, cy = 0.50f, r = 0.05f, size = THIN) // eyes
                    circle(cx = 0.60f, cy = 0.50f, r = 0.05f, size = THIN)
                    dot(x = 0.40f, y = 0.50f)
                    dot(x = 0.60f, y = 0.50f)
                    loop(THIN, 0.46f to 0.60f, 0.54f to 0.60f, 0.50f to 0.65f) // nose
                    arc(
                        cx = 0.455f,
                        cy = 0.65f,
                        rx = 0.045f,
                        ry = 0.035f,
                        fromDegrees = 0f,
                        toDegrees = 180f,
                        size = THIN,
                    )
                    arc(
                        cx = 0.545f,
                        cy = 0.65f,
                        rx = 0.045f,
                        ry = 0.035f,
                        fromDegrees = 0f,
                        toDegrees = 180f,
                        size = THIN,
                    )
                    line(x0 = 0.34f, y0 = 0.60f, x1 = 0.18f, y1 = 0.56f, size = THIN) // whiskers
                    line(x0 = 0.34f, y0 = 0.65f, x1 = 0.18f, y1 = 0.68f, size = THIN)
                    line(x0 = 0.66f, y0 = 0.60f, x1 = 0.82f, y1 = 0.56f, size = THIN)
                    line(x0 = 0.66f, y0 = 0.65f, x1 = 0.82f, y1 = 0.68f, size = THIN)
                },
        )

    public val Sun: ColoringPage =
        ColoringPage(
            id = "sun",
            displayName = "Sun",
            drawing =
                Outline.build {
                    circle(cx = 0.5f, cy = 0.5f, r = 0.24f)
                    for (ray in 0 until RAYS) {
                        val a = ray * FULL_TURN / RAYS
                        loop(MEDIUM, polar(0.25, a - RAY_HALF_ANGLE), polar(0.43, a), polar(0.25, a + RAY_HALF_ANGLE))
                    }
                    dot(x = 0.42f, y = 0.45f)
                    dot(x = 0.58f, y = 0.45f)
                    arc(cx = 0.5f, cy = 0.52f, rx = 0.10f, ry = 0.07f, fromDegrees = 20f, toDegrees = 160f, size = THIN)
                },
        )

    public val Cupcake: ColoringPage =
        ColoringPage(
            id = "cupcake",
            displayName = "Cupcake",
            drawing =
                Outline.build {
                    loop(MEDIUM, 0.26f to 0.55f, 0.74f to 0.55f, 0.64f to 0.86f, 0.36f to 0.86f) // wrapper
                    line(x0 = 0.40f, y0 = 0.58f, x1 = 0.42f, y1 = 0.83f, size = THIN)
                    line(x0 = 0.50f, y0 = 0.58f, x1 = 0.50f, y1 = 0.83f, size = THIN)
                    line(x0 = 0.60f, y0 = 0.58f, x1 = 0.58f, y1 = 0.83f, size = THIN)
                    arc(cx = 0.5f, cy = 0.55f, rx = 0.24f, ry = 0.22f, fromDegrees = 180f, toDegrees = 360f) // frosting
                    arc( // drips
                        cx = 0.36f,
                        cy = 0.55f,
                        rx = 0.05f,
                        ry = 0.04f,
                        fromDegrees = 0f,
                        toDegrees = 180f,
                        size = THIN,
                    )
                    arc(cx = 0.50f, cy = 0.55f, rx = 0.05f, ry = 0.04f, fromDegrees = 0f, toDegrees = 180f, size = THIN)
                    arc(cx = 0.64f, cy = 0.55f, rx = 0.05f, ry = 0.04f, fromDegrees = 0f, toDegrees = 180f, size = THIN)
                    circle(cx = 0.5f, cy = 0.30f, r = 0.04f, size = THIN) // cherry
                    line(x0 = 0.50f, y0 = 0.26f, x1 = 0.55f, y1 = 0.20f, size = THIN)
                    line(x0 = 0.40f, y0 = 0.42f, x1 = 0.43f, y1 = 0.40f, size = THIN) // sprinkles
                    line(x0 = 0.55f, y0 = 0.38f, x1 = 0.58f, y1 = 0.40f, size = THIN)
                    line(x0 = 0.34f, y0 = 0.50f, x1 = 0.37f, y1 = 0.48f, size = THIN)
                    line(x0 = 0.62f, y0 = 0.47f, x1 = 0.65f, y1 = 0.49f, size = THIN)
                },
        )

    public val Rainbow: ColoringPage =
        ColoringPage(
            id = "rainbow",
            displayName = "Rainbow",
            drawing =
                Outline.build {
                    rainbowBand(r = 0.34f, size = MEDIUM)
                    rainbowBand(r = 0.24f, size = THIN)
                    rainbowBand(r = 0.14f, size = MEDIUM)
                    cloud(cx = 0.27f, cy = 0.64f)
                    cloud(cx = 0.73f, cy = 0.64f)
                },
        )

    /** In picker order. */
    public val all: List<ColoringPage> =
        listOf(Pug, Heart, Star, Cat, Sun, Flower, Rainbow, IceCream, Cupcake, Fish, Butterfly, Rocket)

    /** The page saved under [id], or `null` if it was removed from the catalog. */
    public fun byId(id: String): ColoringPage? = all.firstOrNull { it.id == id }

    private const val PETALS = 6
    private const val PETAL_REACH = 0.24
    private const val FULL_TURN = 360.0
    private const val MIDDLE = 0.5f
    private const val RAYS = 8
    private const val RAY_HALF_ANGLE = 13.0

    /** The point [r] from the middle of the page at [degrees] (0 = right, 90 = down). */
    private fun polar(
        r: Double,
        degrees: Double,
    ): Pair<Float, Float> {
        val a = Math.toRadians(degrees)
        return MIDDLE + (r * Math.cos(a)).toFloat() to MIDDLE + (r * Math.sin(a)).toFloat()
    }

    private const val RAINBOW_CENTRE_Y = 0.60f

    /** The bands stop this far above their centre line, under the clouds' top bumps, so no end shows. */
    private const val RAINBOW_END_ABOVE = 0.05f

    /** One band of the rainbow: a half circle of radius [r] whose ends hide inside the clouds. */
    private fun Outline.rainbowBand(
        r: Float,
        size: BrushSize,
    ) {
        val dip = Math.toDegrees(Math.asin((RAINBOW_END_ABOVE / r).toDouble())).toFloat()
        arc(
            cx = 0.5f,
            cy = RAINBOW_CENTRE_Y,
            rx = r,
            ry = r,
            fromDegrees = 180f + dip,
            toDegrees = 360f - dip,
            size = size,
        )
    }

    private const val CLOUD_SIDE = 0.12f
    private const val CLOUD_SIDE_R = 0.06f
    private const val CLOUD_TOP = 0.06f
    private const val CLOUD_TOP_UP = 0.045f
    private const val CLOUD_TOP_R = 0.075f
    private const val CLOUD_BASE = 0.06f

    /** A cloud about 0.36 wide and 0.18 tall around ([cx], [cy]): two bumps on top, flat underneath. */
    private fun Outline.cloud(
        cx: Float,
        cy: Float,
    ) {
        arc(cx - CLOUD_SIDE, cy, CLOUD_SIDE_R, CLOUD_SIDE_R, fromDegrees = 90f, toDegrees = 270f)
        arc(cx - CLOUD_TOP, cy - CLOUD_TOP_UP, CLOUD_TOP_R, CLOUD_TOP_R, fromDegrees = 180f, toDegrees = 360f)
        arc(cx + CLOUD_TOP, cy - CLOUD_TOP_UP, CLOUD_TOP_R, CLOUD_TOP_R, fromDegrees = 180f, toDegrees = 360f)
        arc(cx + CLOUD_SIDE, cy, CLOUD_SIDE_R, CLOUD_SIDE_R, fromDegrees = 270f, toDegrees = 450f)
        line(cx - CLOUD_SIDE, cy + CLOUD_BASE, cx + CLOUD_SIDE, cy + CLOUD_BASE)
    }
}

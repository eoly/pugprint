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

    /** In picker order. */
    public val all: List<ColoringPage> = listOf(Pug, Star, Flower, Fish, Butterfly, Rocket)

    /** The page saved under [id], or `null` if it was removed from the catalog. */
    public fun byId(id: String): ColoringPage? = all.firstOrNull { it.id == id }

    private const val PETALS = 6
    private const val PETAL_REACH = 0.24
    private const val FULL_TURN = 360.0
}

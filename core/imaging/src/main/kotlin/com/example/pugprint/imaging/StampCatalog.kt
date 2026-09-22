package com.example.pugprint.imaging

/**
 * A little picture a kid can put on a sticker, drawn as rows of `#` and `.` like a font glyph.
 *
 * @property id stable key saved with a sticker; never rename once shipped.
 */
public class Stamp(
    public val id: String,
    public val displayName: String,
    public val art: Glyph,
)

/**
 * Every stamp in the app. **To add a stamp, add one [Stamp] to [all]** and draw it — 16 × 16
 * looks best; anything up to [MAX_SIZE] works. `StampCatalogTest` checks the rules and the
 * picker, the renderer and the goldens read this list.
 */
public object StampCatalog {
    /** Largest art a stamp may be, per side. */
    public const val MAX_SIZE: Int = 32

    public val Heart: Stamp =
        Stamp(
            id = "heart",
            displayName = "Heart",
            art =
                Glyph.of(
                    "................",
                    "..####....####..",
                    ".######..######.",
                    "################",
                    "################",
                    "################",
                    "################",
                    ".##############.",
                    "..############..",
                    "...##########...",
                    "....########....",
                    ".....######.....",
                    "......####......",
                    ".......##.......",
                    "................",
                    "................",
                ),
        )

    public val Star: Stamp =
        Stamp(
            id = "star",
            displayName = "Star",
            art =
                Glyph.of(
                    ".......##.......",
                    ".......##.......",
                    "......####......",
                    "......####......",
                    ".....######.....",
                    "################",
                    ".##############.",
                    "..############..",
                    "...##########...",
                    "....########....",
                    "....########....",
                    "...####..####...",
                    "..###......###..",
                    ".##..........##.",
                    "................",
                    "................",
                ),
        )

    public val Paw: Stamp =
        Stamp(
            id = "paw",
            displayName = "Paw",
            art =
                Glyph.of(
                    "................",
                    "...##......##...",
                    "..####....####..",
                    "..####....####..",
                    "...##......##...",
                    ".##..........##.",
                    "####........####",
                    "####........####",
                    ".##...####...##.",
                    ".....######.....",
                    "....########....",
                    "...##########...",
                    "...##########...",
                    "....########....",
                    ".....######.....",
                    "................",
                ),
        )

    public val Smiley: Stamp =
        Stamp(
            id = "smiley",
            displayName = "Smiley",
            art =
                Glyph.of(
                    ".....######.....",
                    "...##......##...",
                    "..#..........#..",
                    ".#............#.",
                    ".#...##..##...#.",
                    "#....##..##....#",
                    "#..............#",
                    "#..............#",
                    "#..#........#..#",
                    "#...#......#...#",
                    ".#...######...#.",
                    ".#............#.",
                    "..#..........#..",
                    "...##......##...",
                    ".....######.....",
                    "................",
                ),
        )

    public val Sun: Stamp =
        Stamp(
            id = "sun",
            displayName = "Sun",
            art =
                Glyph.of(
                    ".......##.......",
                    ".......##.......",
                    ".##....##....##.",
                    "..##..####..##..",
                    "...#.######.#...",
                    "....########....",
                    "...##########...",
                    "##.##########.##",
                    "##.##########.##",
                    "...##########...",
                    "....########....",
                    "...#.######.#...",
                    "..##..####..##..",
                    ".##....##....##.",
                    ".......##.......",
                    ".......##.......",
                ),
        )

    public val Bolt: Stamp =
        Stamp(
            id = "bolt",
            displayName = "Zap",
            art =
                Glyph.of(
                    ".........####...",
                    "........####....",
                    ".......####.....",
                    "......####......",
                    ".....####.......",
                    "....########....",
                    "...#######......",
                    "......####......",
                    ".....####.......",
                    "....####........",
                    "...####.........",
                    "..####..........",
                    ".###............",
                    ".##.............",
                    "#...............",
                    "................",
                ),
        )

    /** In picker order. */
    public val all: List<Stamp> = listOf(Heart, Star, Paw, Smiley, Sun, Bolt)

    /** The stamp saved under [id], or `null` if it was removed from the catalog. */
    public fun byId(id: String): Stamp? = all.firstOrNull { it.id == id }
}

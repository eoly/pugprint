package com.example.pugprint.imaging

/**
 * Where on the head's 384-dot canvas a roll's picture goes, so it lands centred on the label.
 * All in print dots (8 per mm). Measured on paper: the head cannot print further left or higher
 * than it does, so centring means leaving white on the right and the top.
 */
public data class PrintPlacement(
    val topMarginRows: Int = 0,
    val leftInsetDots: Int = 0,
    val rightInsetDots: Int = 0,
) {
    init {
        require(topMarginRows >= 0 && leftInsetDots >= 0 && rightInsetDots >= 0) { "Margins must not be negative" }
    }
}

/** The outline of one sticker. A plain roll and a square label are rectangles; a round label is a circle. */
public enum class LabelShape {
    RECTANGLE,

    /** The picture is clipped to the circle inscribed in its square, so nothing prints on the backing. */
    CIRCLE,
}

/**
 * One kind of paper. A die-cut label roll fixes the sticker's size and shape; a plain roll
 * lets the picture be as tall as it likes. **To add a roll, add one [StickerRoll] to
 * [StickerRollCatalog.all]** with a ruler in hand.
 *
 * @property id stable key saved in settings; never rename once shipped.
 * @property labelMm the label's printable face (and its outline), or `null` for continuous paper.
 * @property gapMm paper between two labels (informational; the firmware feeds to the next label itself).
 * @property placement where the picture sits on the canvas so it prints centred.
 */
public data class StickerRoll(
    val id: String,
    val displayName: String,
    val labelMm: LabelSize? = null,
    val gapMm: Float = 0f,
    val placement: PrintPlacement = PrintPlacement(),
) {
    /** A label's printable face in millimetres; a [LabelShape.CIRCLE] is [widthMm] across. */
    public data class LabelSize(
        val widthMm: Float,
        val heightMm: Float,
        val shape: LabelShape = LabelShape.RECTANGLE,
    ) {
        init {
            require(widthMm > 0f && heightMm > 0f) { "A label must have a size" }
            require(shape != LabelShape.CIRCLE || widthMm == heightMm) { "A round label is as tall as it is wide" }
        }
    }

    public val isLabel: Boolean get() = labelMm != null

    /** The sticker's outline; [LabelShape.RECTANGLE] for plain paper. */
    public val shape: LabelShape get() = labelMm?.shape ?: LabelShape.RECTANGLE

    /** Dots across the picture: the head minus the insets. */
    public val contentWidth: Int = HEAD_DOTS - placement.leftInsetDots - placement.rightInsetDots

    /**
     * Rows the picture may take: for a label, what is left of one canvas after the top margin
     * (the canvas is [HEAD_DOTS] rows, which the standard roll proved fits one label); for a plain
     * roll, the pipeline's safety cap.
     */
    public val contentHeight: Int = if (isLabel) HEAD_DOTS - placement.topMarginRows else ImagePipeline.MAX_ROWS

    init {
        require(contentWidth > 0 && contentHeight > 0) { "$id: placement leaves no room to print" }
        require(shape != LabelShape.CIRCLE || contentWidth == contentHeight) {
            "$id: a round label needs square content"
        }
    }

    /** The dots for [sticker] on this roll: rendered at the roll's size and cut to its [shape]; see [place]. */
    public fun render(sticker: Sticker): MonoBitmap =
        StickerRenderer.render(sticker, width = contentWidth, maxRows = contentHeight, shape = shape)

    /**
     * Pads [content] (exactly [contentWidth] wide) onto the head-wide canvas: white to the left,
     * right and above; for a label also below, so one print is exactly one canvas.
     */
    public fun place(content: MonoBitmap): MonoBitmap {
        require(content.width == contentWidth) { "$id: expected $contentWidth-dot content, got ${content.width}" }
        require(
            content.height <= contentHeight,
        ) { "$id: content is ${content.height} rows, at most $contentHeight fit" }
        val untouched = placement == PrintPlacement() && !isLabel
        if (untouched) return content
        val height = if (isLabel) HEAD_DOTS else content.height + placement.topMarginRows
        val canvas = BitCanvas(HEAD_DOTS, height)
        canvas.drawBlack(BitCanvas.from(content), placement.leftInsetDots, placement.topMarginRows)
        return canvas.toMonoBitmap()
    }

    public companion object {
        /** Dots across the print head; also the rows in one label canvas. */
        public const val HEAD_DOTS: Int = ImagePipeline.PRINT_WIDTH
    }
}

/**
 * Every roll the app knows. Adding one is adding an entry; `StickerRollCatalogTest` checks it.
 */
public object StickerRollCatalog {
    /**
     * The standard square sticker roll, measured 2026-09-22: 1 15/16 in (49.2 mm) square labels
     * with a 1/2 in (12.7 mm) gap, serrated halfway. A full-width 384-row print landed 3.2 mm from
     * the left edge, 0.8 mm from the right, flush with the top and 3.2 mm short of the bottom, so
     * the picture is 365 dots square with 19 dots of white on the right and 19 rows on top.
     */
    public val SquareStandard: StickerRoll =
        StickerRoll(
            id = "square-49",
            displayName = "Square stickers",
            labelMm = StickerRoll.LabelSize(widthMm = 49.2f, heightMm = 49.2f),
            gapMm = 12.7f,
            placement = PrintPlacement(topMarginRows = SQUARE_TOP_ROWS, rightInsetDots = SQUARE_RIGHT_DOTS),
        )

    /**
     * Round stickers on the same 58 mm web as [SquareStandard]: 49.2 mm across, so the label sits
     * where the square one does and the picture gets the same 365-dot placement, then is cut to the
     * inscribed circle. **Not yet measured on paper** (2026-09-22): if a print lands off-centre,
     * adjust this roll's [PrintPlacement] with a ruler, not the square roll's.
     */
    public val CircleStandard: StickerRoll =
        StickerRoll(
            id = "circle-49",
            displayName = "Round stickers",
            labelMm = StickerRoll.LabelSize(widthMm = 49.2f, heightMm = 49.2f, shape = LabelShape.CIRCLE),
            gapMm = 12.7f,
            placement = PrintPlacement(topMarginRows = SQUARE_TOP_ROWS, rightInsetDots = SQUARE_RIGHT_DOTS),
        )

    /** Plain 58 mm thermal paper: any shape, up to the pipeline's length cap. */
    public val Continuous: StickerRoll = StickerRoll(id = "continuous", displayName = "Plain roll")

    /** In picker order. */
    public val all: List<StickerRoll> = listOf(SquareStandard, CircleStandard, Continuous)

    public val default: StickerRoll = SquareStandard

    /** The roll saved under [id], or [default] if the id is unknown. */
    public fun byId(id: String?): StickerRoll = all.firstOrNull { it.id == id } ?: default

    private const val SQUARE_TOP_ROWS = 19
    private const val SQUARE_RIGHT_DOTS = 19
}

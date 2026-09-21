package com.example.pugprint.printer

/** One transport write: [bytes] go out in a single BLE write, then the sender waits [pauseAfterMillis]. */
public class PrinterWrite(
    /** Human-readable name used in logs and golden fixtures, e.g. `raster block 3/48`. */
    public val label: String,
    public val bytes: ByteArray,
    public val pauseAfterMillis: Long,
) {
    override fun toString(): String = "PrinterWrite($label, ${bytes.size} B, pause $pauseAfterMillis ms)"
}

/** Pacing the Phase 0 spike proved safe over BLE (docs/PRINTER_PROTOCOL.md § BLE constraints). */
public object PrintTiming {
    /** Gap between consecutive raster blocks. Faster is untested. */
    public const val BLOCK_GAP_MILLIS: Long = 20

    /** Pause between the last raster block and the feed, as the vendor app does. */
    public const val BEFORE_FEED_MILLIS: Long = 250
}

/**
 * Knobs that rarely change between jobs.
 *
 * @property rowsPerBlock rows per `GS v 0` block; 1 is the only value proven reliable over BLE.
 * @property copies vendor `GS I 0xF8` count; the command is omitted when 1.
 * @property speed vendor `GS I 0xF1` value, only for old private-factory firmware ([DensityProfile.legacySpeed]).
 * @property feedLines line feeds after the image.
 */
public data class PrintOptions(
    val rowsPerBlock: Int = 1,
    val copies: Int = 1,
    val speed: Int? = null,
    val feedLines: Int = PrinterCommands.DEFAULT_FEED_LINES,
)

/**
 * The byte-exact job sequence the vendor app sends, adapted to BLE:
 * density · [speed] · [copies] · init · raster blocks · (250 ms) · feed.
 *
 * @param rows packed 1-bpp rows, top to bottom, each [PrinterSpec.BYTES_PER_ROW] bytes for a 384-dot head.
 * @param density the `GS I 0xF0` heat value, e.g. `DensityProfile.PUBLIC.value(DensityLevel.MEDIUM)`.
 */
public class PrintJob(
    public val rows: List<ByteArray>,
    public val density: Int,
    public val options: PrintOptions = PrintOptions(),
) {
    init {
        require(rows.isNotEmpty()) { "Nothing to print: no rows" }
    }

    /** The writes to send, in order, each with the pause that must follow it. */
    public fun writes(): List<PrinterWrite> {
        val writes = ArrayList<PrinterWrite>(rows.size + PREAMBLE_CAPACITY)
        writes += PrinterWrite("density", PrinterCommands.density(density), 0)
        options.speed?.let { writes += PrinterWrite("speed", PrinterCommands.speed(it), 0) }
        if (options.copies > 1) {
            writes += PrinterWrite("copies", PrinterCommands.copies(options.copies), 0)
        }
        writes += PrinterWrite("init", PrinterCommands.init(), 0)
        val blocks = RasterBlock.split(rows, options.rowsPerBlock)
        blocks.forEachIndexed { index, block ->
            val last = index == blocks.lastIndex
            val pause = if (last) PrintTiming.BEFORE_FEED_MILLIS else PrintTiming.BLOCK_GAP_MILLIS
            writes += PrinterWrite("raster block ${index + 1}/${blocks.size}", block, pause)
        }
        writes += PrinterWrite("feed", PrinterCommands.feed(options.feedLines), 0)
        return writes
    }

    private companion object {
        const val PREAMBLE_CAPACITY = 5
    }
}

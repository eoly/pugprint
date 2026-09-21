package com.example.pugprint.printer

/**
 * A host → printer command as recovered from the byte stream by [CommandDecoder].
 * The inverse of [PrinterCommands] / [PrinterQueries] / [RasterBlock]; used by
 * [PrinterEmulator] and by transport tests that assert what would reach the printer.
 */
public sealed interface PrinterCommand {
    /** `GS I 0xF0 n`. */
    public data class Density(
        val value: Int,
    ) : PrinterCommand

    /** `GS I 0xF1 n`. */
    public data class Speed(
        val value: Int,
    ) : PrinterCommand

    /** `GS I 0xF8 n`. */
    public data class Copies(
        val count: Int,
    ) : PrinterCommand

    /** `ESC @`. */
    public data object Init : PrinterCommand

    /** One `GS v 0` block. */
    public class Raster(
        public val bytesPerRow: Int,
        public val rows: List<ByteArray>,
    ) : PrinterCommand {
        override fun toString(): String = "Raster(${rows.size} rows × $bytesPerRow B)"
    }

    /** A bare `LF`. */
    public data object LineFeed : PrinterCommand

    /** One of the [PrinterQueries]. */
    public enum class Query : PrinterCommand {
        STATUS,
        SERIAL,
        PRODUCT,
        PAPER,
    }

    /** `DLE EOT n`. */
    public data class RealtimeStatus(
        val kind: Int,
    ) : PrinterCommand

    /** A command that ended before its declared length; the printer discards it. */
    public data class Truncated(
        val expectedBytes: Int,
        val actualBytes: Int,
    ) : PrinterCommand

    /** A byte the decoder does not understand; skipped one byte at a time. */
    public data class Unknown(
        val byte: Int,
    ) : PrinterCommand
}

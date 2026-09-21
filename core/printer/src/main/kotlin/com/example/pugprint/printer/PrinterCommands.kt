package com.example.pugprint.printer

import com.example.pugprint.printer.ControlBytes.ESC
import com.example.pugprint.printer.ControlBytes.GS
import com.example.pugprint.printer.ControlBytes.LF
import com.example.pugprint.printer.ControlBytes.MAX_BYTE

/**
 * Byte-exact encoders for the Hello Blink command set confirmed in the Phase 0 spike
 * (docs/PRINTER_PROTOCOL.md § Print sequence). Status queries live in [PrinterQueries].
 *
 * Every function returns a fresh array so callers may hand it straight to a transport.
 * Raster data is built by [RasterBlock]; a whole job is composed by [PrintJob].
 */
public object PrinterCommands {
    /** Lines the vendor app feeds after the raster so the sticker clears the tear bar. */
    public const val DEFAULT_FEED_LINES: Int = 4

    /** Speed value the vendor app sends to "old" private-factory firmware only. */
    public const val LEGACY_SPEED: Int = 0x1E

    private const val GS_I: Byte = 0x49
    private const val GS_I_DENSITY: Byte = 0xF0.toByte()
    private const val GS_I_SPEED: Byte = 0xF1.toByte()
    private const val GS_I_COPIES: Byte = 0xF8.toByte()
    private const val ESC_AT: Byte = 0x40
    private const val MIN_COPIES: Int = 2

    /** `ESC @` — initialise the print engine. Sent once per job, after [density]. */
    public fun init(): ByteArray = byteArrayOf(ESC, ESC_AT)

    /**
     * `GS I 0xF0 n` — head heat. Must precede [init]; without it nothing prints.
     * Use [DensityProfile] to pick [value] for the connected printer.
     */
    public fun density(value: Int): ByteArray = gsI(GS_I_DENSITY, value)

    /** `GS I 0xF1 n` — print speed. The vendor app sends this only to old private-factory firmware. */
    public fun speed(value: Int): ByteArray = gsI(GS_I_SPEED, value)

    /** `GS I 0xF8 n` — number of copies. The vendor app only sends it when [count] > 1. */
    public fun copies(count: Int): ByteArray {
        require(count in MIN_COPIES..MAX_BYTE) { "copies must be in $MIN_COPIES..$MAX_BYTE, got $count" }
        return gsI(GS_I_COPIES, count)
    }

    /** [lines] × `LF` — advance the paper. Sent 250 ms after the last raster block. */
    public fun feed(lines: Int = DEFAULT_FEED_LINES): ByteArray {
        require(lines in 1..MAX_BYTE) { "feed lines must be in 1..$MAX_BYTE, got $lines" }
        return ByteArray(lines) { LF }
    }

    private fun gsI(
        function: Byte,
        value: Int,
    ): ByteArray {
        require(value in 0..MAX_BYTE) { "value must be in 0..$MAX_BYTE, got $value" }
        return byteArrayOf(GS, GS_I, function, value.toByte())
    }
}

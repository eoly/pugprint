package com.example.pugprint.printer

import com.example.pugprint.printer.ControlBytes.DLE
import com.example.pugprint.printer.ControlBytes.EOT
import com.example.pugprint.printer.ControlBytes.ESC
import com.example.pugprint.printer.ControlBytes.GS
import com.example.pugprint.printer.ControlBytes.LF
import com.example.pugprint.printer.ControlBytes.MAX_BYTE
import com.example.pugprint.printer.ControlBytes.RS

/**
 * Decodes one transport write into the [PrinterCommand]s it contains.
 *
 * Each write is decoded on its own, mirroring the hardware: a `GS v 0` block that does
 * not end within the write comes back as [PrinterCommand.Truncated] rather than being
 * continued by the next write (docs/PRINTER_PROTOCOL.md § BLE constraints).
 */
public object CommandDecoder {
    private const val BITS_PER_BYTE = 8
    private const val GS_I: Int = 0x49
    private const val GS_V: Int = 0x76
    private const val GS_G: Int = 0x67
    private const val GS_R: Int = 0x72
    private const val ESC_AT: Int = 0x40
    private const val RS_G: Int = 0x47
    private const val DENSITY: Int = 0xF0
    private const val SPEED: Int = 0xF1
    private const val COPIES: Int = 0xF8
    private const val STATUS_ARG: Int = 0x03
    private const val SERIAL_ARG: Int = 0x39
    private const val PRODUCT_ARG: Int = 0x69
    private const val PAPER_ARG: Int = 0x01
    private const val TWO_ARGS = 2
    private const val THREE_ARGS = 3

    public fun decode(write: ByteArray): List<PrinterCommand> {
        val out = ArrayList<PrinterCommand>()
        val reader = Reader(write)
        while (reader.remaining > 0) {
            val start = reader.pos
            val lead = reader.byte()
            val command = decodeBody(lead, reader)
            if (command == null) {
                reader.pos = start + 1
                out += PrinterCommand.Unknown(lead)
            } else {
                out += command
            }
        }
        return out
    }

    /** Decodes the command whose lead byte was [lead]; `null` means "not a command we know". */
    private fun decodeBody(
        lead: Int,
        reader: Reader,
    ): PrinterCommand? =
        when (lead.toByte()) {
            LF -> PrinterCommand.LineFeed
            ESC -> PrinterCommand.Init.takeIf { reader.take(1) && reader.byte() == ESC_AT }
            RS ->
                PrinterCommand.Query.STATUS.takeIf {
                    reader.take(TWO_ARGS) &&
                        reader.byte() == RS_G &&
                        reader.byte() == STATUS_ARG
                }
            DLE ->
                if (reader.take(TWO_ARGS) &&
                    reader.byte() == EOT.toInt()
                ) {
                    PrinterCommand.RealtimeStatus(reader.byte())
                } else {
                    null
                }
            GS -> if (reader.take(TWO_ARGS)) decodeGs(reader.byte(), reader.byte(), reader) else null
            else -> null
        }

    private fun decodeGs(
        function: Int,
        arg: Int,
        reader: Reader,
    ): PrinterCommand? =
        when (function) {
            GS_I -> if (reader.take(1)) gsI(arg, reader.byte()) else null
            GS_G ->
                when (arg) {
                    SERIAL_ARG -> PrinterCommand.Query.SERIAL
                    PRODUCT_ARG -> PrinterCommand.Query.PRODUCT
                    else -> null
                }
            GS_R -> PrinterCommand.Query.PAPER.takeIf { arg == PAPER_ARG }
            GS_V -> if (arg == RasterBlock.MODE_NORMAL_ARG) raster(reader) else null
            else -> null
        }

    private fun gsI(
        function: Int,
        value: Int,
    ): PrinterCommand? =
        when (function) {
            DENSITY -> PrinterCommand.Density(value)
            SPEED -> PrinterCommand.Speed(value)
            COPIES -> PrinterCommand.Copies(value)
            else -> null
        }

    /** Called after `GS v 0`; reads `m xL xH yL yH` and the pixel data. */
    private fun raster(reader: Reader): PrinterCommand? {
        val headerConsumed = THREE_ARGS
        if (!reader.take(RasterBlock.HEADER_SIZE - headerConsumed) || reader.byte() != 0) return null
        val bytesPerRow = reader.u16()
        val rowCount = reader.u16()
        val dataLength = bytesPerRow * rowCount
        return if (reader.take(dataLength)) {
            PrinterCommand.Raster(bytesPerRow, List(rowCount) { reader.bytes(bytesPerRow) })
        } else {
            val consumed = RasterBlock.HEADER_SIZE + reader.remaining
            reader.skipToEnd()
            PrinterCommand.Truncated(expectedBytes = RasterBlock.HEADER_SIZE + dataLength, actualBytes = consumed)
        }
    }

    private class Reader(
        private val buf: ByteArray,
    ) {
        var pos: Int = 0
        val remaining: Int get() = buf.size - pos

        /** True when at least [count] more bytes can be read. */
        fun take(count: Int): Boolean = remaining >= count

        fun byte(): Int = buf[pos++].toInt() and MAX_BYTE

        fun u16(): Int {
            val low = byte()
            return low or (byte() shl BITS_PER_BYTE)
        }

        fun bytes(count: Int): ByteArray = buf.copyOfRange(pos, pos + count).also { pos += count }

        fun skipToEnd() {
            pos = buf.size
        }
    }
}

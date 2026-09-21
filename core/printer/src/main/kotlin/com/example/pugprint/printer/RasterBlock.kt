package com.example.pugprint.printer

import com.example.pugprint.printer.ControlBytes.GS
import com.example.pugprint.printer.ControlBytes.MAX_BYTE

/**
 * `GS v 0` raster image blocks: `1d 76 30 m xL xH yL yH` followed by `x × y` packed bytes,
 * 1 bpp, MSB-first, `1` = black, pixel 0 at the left edge.
 *
 * Over BLE the printer drops rows from multi-row blocks and discards any block that spans
 * two writes (docs/PRINTER_PROTOCOL.md § BLE constraints), so [split] defaults to one row
 * per block and refuses more than [MAX_ROWS_PER_BLOCK].
 */
public object RasterBlock {
    /** Header length before the packed pixel data. */
    public const val HEADER_SIZE: Int = 8

    /** Most rows a 384-dot block can hold and still fit one 245-byte BLE write. Only 1 is proven reliable. */
    public const val MAX_ROWS_PER_BLOCK: Int = 4

    private const val GS_V: Byte = 0x76
    private const val MODE_NORMAL: Byte = 0x30
    private const val MAX_DIMENSION: Int = 0xFFFF
    private const val BITS_PER_BYTE: Int = 8

    /** Encodes [rows] as one block. Every row must have the same, non-zero length. */
    public fun encode(rows: List<ByteArray>): ByteArray {
        require(rows.isNotEmpty()) { "A raster block needs at least one row" }
        val bytesPerRow = rows.first().size
        require(bytesPerRow in 1..MAX_DIMENSION) { "bytes per row must be in 1..$MAX_DIMENSION, got $bytesPerRow" }
        require(rows.size <= MAX_DIMENSION) { "at most $MAX_DIMENSION rows per block, got ${rows.size}" }
        rows.forEachIndexed { index, row ->
            require(row.size == bytesPerRow) { "row $index has ${row.size} bytes, expected $bytesPerRow" }
        }
        val out = ByteArray(HEADER_SIZE + bytesPerRow * rows.size)
        val header =
            byteArrayOf(GS, GS_V, MODE_NORMAL, 0, low(bytesPerRow), high(bytesPerRow), low(rows.size), high(rows.size))
        check(header.size == HEADER_SIZE)
        header.copyInto(out)
        var offset = HEADER_SIZE
        for (row in rows) {
            row.copyInto(out, offset)
            offset += bytesPerRow
        }
        return out
    }

    /** Splits [rows] into consecutive blocks of [rowsPerBlock] rows (the last may be shorter). */
    public fun split(
        rows: List<ByteArray>,
        rowsPerBlock: Int = 1,
    ): List<ByteArray> {
        require(rowsPerBlock in 1..MAX_ROWS_PER_BLOCK) {
            "rowsPerBlock must be in 1..$MAX_ROWS_PER_BLOCK over BLE, got $rowsPerBlock"
        }
        require(rows.isNotEmpty()) { "Nothing to print: no rows" }
        return rows.chunked(rowsPerBlock).map(::encode)
    }

    private fun low(value: Int): Byte = (value and MAX_BYTE).toByte()

    private fun high(value: Int): Byte = (value ushr BITS_PER_BYTE).toByte()
}

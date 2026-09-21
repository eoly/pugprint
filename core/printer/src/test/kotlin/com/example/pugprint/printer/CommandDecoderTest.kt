package com.example.pugprint.printer

import com.example.pugprint.printer.Fixtures.hex
import com.example.pugprint.printer.Fixtures.parseHex
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class CommandDecoderTest {
    @Test
    fun `decodes every encoder output back to its command`() {
        assertEquals(listOf(PrinterCommand.Density(25)), CommandDecoder.decode(PrinterCommands.density(25)))
        assertEquals(listOf(PrinterCommand.Speed(0x1e)), CommandDecoder.decode(PrinterCommands.speed(0x1e)))
        assertEquals(listOf(PrinterCommand.Copies(3)), CommandDecoder.decode(PrinterCommands.copies(3)))
        assertEquals(listOf(PrinterCommand.Init), CommandDecoder.decode(PrinterCommands.init()))
        assertEquals(List(4) { PrinterCommand.LineFeed }, CommandDecoder.decode(PrinterCommands.feed()))
        assertEquals(listOf(PrinterCommand.Query.STATUS), CommandDecoder.decode(PrinterQueries.queryStatus()))
        assertEquals(listOf(PrinterCommand.Query.SERIAL), CommandDecoder.decode(PrinterQueries.querySerial()))
        assertEquals(listOf(PrinterCommand.Query.PRODUCT), CommandDecoder.decode(PrinterQueries.queryProduct()))
        assertEquals(listOf(PrinterCommand.Query.PAPER), CommandDecoder.decode(PrinterQueries.queryPaper()))
        assertEquals(listOf(PrinterCommand.RealtimeStatus(4)), CommandDecoder.decode(PrinterQueries.realtimeStatus(4)))
    }

    @Test
    fun `decodes a raster block into its rows`() {
        val rows = listOf(ByteArray(48) { 0xFF.toByte() }, ByteArray(48) { it.toByte() })

        val decoded = CommandDecoder.decode(RasterBlock.encode(rows))

        val raster = assertInstanceOf(PrinterCommand.Raster::class.java, decoded.single())
        assertEquals(48, raster.bytesPerRow)
        assertEquals(rows.map(::hex), raster.rows.map(::hex))
    }

    @Test
    fun `decodes several commands from one buffer in order`() {
        val buffer =
            PrinterCommands.density(25) + PrinterCommands.init() + RasterBlock.encode(listOf(ByteArray(48))) +
                PrinterCommands.feed(2)

        val decoded = CommandDecoder.decode(buffer)

        assertEquals(
            listOf("Density(value=25)", "Init", "Raster(1 rows × 48 B)", "LineFeed", "LineFeed"),
            decoded.map { it.toString() },
        )
    }

    @Test
    fun `a block cut off by the end of the write is reported truncated`() {
        val block = RasterBlock.encode(listOf(ByteArray(48)))

        val decoded = CommandDecoder.decode(block.copyOf(30))

        assertEquals(listOf(PrinterCommand.Truncated(expectedBytes = 56, actualBytes = 30)), decoded)
    }

    @Test
    fun `unknown bytes are skipped one at a time and decoding resumes`() {
        val decoded = CommandDecoder.decode(parseHex("ff 1b 41 1b 40 1d"))

        assertEquals(
            listOf(
                PrinterCommand.Unknown(0xff),
                PrinterCommand.Unknown(0x1b),
                PrinterCommand.Unknown(0x41),
                PrinterCommand.Init,
                PrinterCommand.Unknown(0x1d),
            ),
            decoded,
        )
    }
}

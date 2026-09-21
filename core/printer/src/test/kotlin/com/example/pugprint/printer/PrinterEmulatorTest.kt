package com.example.pugprint.printer

import com.example.pugprint.printer.Fixtures.hex
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PrinterEmulatorTest {
    @ParameterizedTest
    @ValueSource(strings = ["black_48_rows", "stripe_48_rows", "widths_48_rows", "pug_arrow"])
    fun `replaying a vendor fixture reproduces the PBM rows`(name: String) {
        val emulator = PrinterEmulator()

        Fixtures.vendorJob(name).forEach { emulator.write(it.bytes) }

        assertEquals(emptyList<String>(), emulator.violations)
        assertEquals(25, emulator.density)
        assertEquals(1, emulator.initCount)
        assertEquals(4, emulator.feedLines)
        assertEquals(Fixtures.pbmRows(name).map(::hex), emulator.rows.map(::hex))
    }

    @Test
    fun `PrintJob round-trips through the emulator`() {
        val rows = Fixtures.pbmRows("pug_arrow")
        val emulator = PrinterEmulator()

        val replies = emulator.print(PrintJob(rows, density = 30, options = PrintOptions(copies = 2)))

        assertTrue(replies.isEmpty())
        assertEquals(emptyList<String>(), emulator.violations)
        assertEquals(30, emulator.density)
        assertEquals(2, emulator.copies)
        assertNull(emulator.speed)
        assertEquals(rows.map(::hex), emulator.rows.map(::hex))
    }

    @Test
    fun `rows before a density command are dropped`() {
        val emulator = PrinterEmulator()

        emulator.write(PrinterCommands.init())
        emulator.write(RasterBlock.encode(listOf(ByteArray(48))))

        assertEquals(0, emulator.rows.size)
        assertEquals(listOf("raster block before any density command"), emulator.violations)
    }

    @Test
    fun `a block split across writes is dropped`() {
        val emulator = PrinterEmulator()
        val block = RasterBlock.encode(listOf(ByteArray(48) { 1 }))

        emulator.write(PrinterCommands.density(25))
        emulator.write(block.copyOfRange(0, 20))
        emulator.write(block.copyOfRange(20, block.size))

        assertEquals(0, emulator.rows.size)
        assertEquals(1, emulator.violations.count { it.contains("split across writes") })
    }

    @Test
    fun `multi-row blocks are rejected unless the emulator allows them`() {
        val rows = List(4) { ByteArray(48) }
        val strict = PrinterEmulator()
        val lenient = PrinterEmulator(maxRowsPerBlock = 4)

        strict.write(PrinterCommands.density(25))
        strict.write(RasterBlock.encode(rows))
        lenient.write(PrinterCommands.density(25))
        lenient.write(RasterBlock.encode(rows))

        assertEquals(0, strict.rows.size)
        assertEquals(listOf("raster block with 4 rows; only 1 reliable over BLE"), strict.violations)
        assertEquals(4, lenient.rows.size)
        assertEquals(emptyList<String>(), lenient.violations)
    }

    @Test
    fun `queries are answered like the reference unit and parse back`() {
        val emulator = PrinterEmulator()

        val status = PrinterReply.parse(emulator.write(PrinterQueries.queryStatus()).single())
        val serial = PrinterReply.parse(emulator.write(PrinterQueries.querySerial()).single())
        val product = PrinterReply.parse(emulator.write(PrinterQueries.queryProduct()).single())

        assertEquals(PrinterReply.Status("H1.0", "V1.01", 7540, 384), status)
        assertEquals(PrinterReply.Serial("HBHW250000001234"), serial)
        assertEquals(PrinterReply.Product(801), product)
        assertEquals("00", hex(emulator.write(PrinterQueries.queryPaper()).single()))
        assertEquals(
            listOf("1e", "1a", "12", "12"),
            (1..4).map { hex(emulator.write(PrinterQueries.realtimeStatus(it)).single()) },
        )
        assertInstanceOf(
            PrinterReply.Unknown::class.java,
            PrinterReply.parse(emulator.write(PrinterQueries.queryPaper()).single()),
        )
    }

    @Test
    fun `identity is configurable`() {
        val emulator = PrinterEmulator(PrinterEmulator.Identity(firmwareVersion = "V1.20", productId = 12))

        val status = PrinterReply.parse(emulator.write(PrinterQueries.queryStatus()).single()) as PrinterReply.Status
        val product = PrinterReply.parse(emulator.write(PrinterQueries.queryProduct()).single()) as PrinterReply.Product

        assertEquals(120, status.firmwareNumber)
        assertEquals(FactoryType.PRIVATE, product.factory)
        assertEquals(DensityProfile.PRIVATE_NEW, DensityProfile.forPrinter(product.factory, status.firmwareNumber))
    }
}

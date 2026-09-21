package com.example.pugprint.printer

import com.example.pugprint.printer.Fixtures.hex
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PrinterCommandsTest {
    @Test
    fun `print sequence commands match docs PRINTER_PROTOCOL`() {
        assertEquals("1d 49 f0 19", hex(PrinterCommands.density(25)))
        assertEquals("1d 49 f1 1e", hex(PrinterCommands.speed(PrinterCommands.LEGACY_SPEED)))
        assertEquals("1d 49 f8 02", hex(PrinterCommands.copies(2)))
        assertEquals("1b 40", hex(PrinterCommands.init()))
        assertEquals("0a 0a 0a 0a", hex(PrinterCommands.feed()))
        assertEquals("0a", hex(PrinterCommands.feed(1)))
    }

    @Test
    fun `query commands match docs PRINTER_PROTOCOL`() {
        assertEquals("1e 47 03", hex(PrinterQueries.queryStatus()))
        assertEquals("1d 67 39", hex(PrinterQueries.querySerial()))
        assertEquals("1d 67 69", hex(PrinterQueries.queryProduct()))
        assertEquals("10 04 01", hex(PrinterQueries.realtimeStatus(1)))
        assertEquals("10 04 04", hex(PrinterQueries.realtimeStatus(4)))
        assertEquals("1d 72 01", hex(PrinterQueries.queryPaper()))
    }

    @Test
    fun `single-byte parameters are range-checked`() {
        assertThrows(IllegalArgumentException::class.java) { PrinterCommands.density(256) }
        assertThrows(IllegalArgumentException::class.java) { PrinterCommands.density(-1) }
        assertThrows(IllegalArgumentException::class.java) { PrinterCommands.copies(1) }
        assertThrows(IllegalArgumentException::class.java) { PrinterCommands.feed(0) }
        assertThrows(IllegalArgumentException::class.java) { PrinterQueries.realtimeStatus(0) }
        assertThrows(IllegalArgumentException::class.java) { PrinterQueries.realtimeStatus(5) }
    }

    @Test
    fun `each call returns a fresh array`() {
        val a = PrinterCommands.init()
        a[0] = 0
        assertEquals("1b 40", hex(PrinterCommands.init()))
    }
}

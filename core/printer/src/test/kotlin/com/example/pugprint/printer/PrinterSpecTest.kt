package com.example.pugprint.printer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.UUID

class PrinterSpecTest {
    @Test
    fun `a 384-dot row packs into 48 bytes`() {
        assertEquals(48, PrinterSpec.BYTES_PER_ROW)
        assertEquals(0, PrinterSpec.DOTS_PER_LINE % 8)
    }

    @Test
    fun `head is 48 mm wide`() {
        assertEquals(48, PrinterSpec.DOTS_PER_LINE / PrinterSpec.DOTS_PER_MM)
    }

    @ParameterizedTest
    @ValueSource(strings = [HelloBlinkUuids.SERVICE, HelloBlinkUuids.RX, HelloBlinkUuids.TX])
    fun `uuids are well-formed and in the Microchip 49535343 range`(raw: String) {
        val uuid = UUID.fromString(raw)
        assertEquals(raw, uuid.toString())
        assertEquals("49535343", raw.substring(0, 8))
    }
}

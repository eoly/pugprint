package com.example.pugprint.printer

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EscPosStatusTest {
    @Test
    fun `idle replies from the reference unit report no faults`() {
        // DLE EOT 1..4 → 1e 1a 12 12; GS r 1 → 00
        assertFalse(EscPosStatus.coverOpen(0x1a))
        assertFalse(EscPosStatus.hasError(0x12))
        assertFalse(EscPosStatus.paperOut(0x12))
        assertFalse(EscPosStatus.paperNearEnd(0x12))
        assertFalse(EscPosStatus.paperOutFromGsR(0x00))
    }

    @Test
    fun `fault bits follow the ESC POS specification`() {
        assertTrue(EscPosStatus.coverOpen((0x12 or 0x04).toByte()))
        assertTrue(EscPosStatus.hasError((0x12 or 0x40).toByte()))
        assertTrue(EscPosStatus.hasError((0x12 or 0x08).toByte()))
        assertTrue(EscPosStatus.paperOut((0x12 or 0x60).toByte()))
        assertTrue(EscPosStatus.paperNearEnd((0x12 or 0x0c).toByte()))
        assertFalse(EscPosStatus.paperOut((0x12 or 0x0c).toByte()))
        assertTrue(EscPosStatus.paperOutFromGsR(0x0c))
        assertFalse(EscPosStatus.paperOutFromGsR(0x03))
    }
}

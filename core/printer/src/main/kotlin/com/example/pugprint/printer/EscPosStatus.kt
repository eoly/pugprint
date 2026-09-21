package com.example.pugprint.printer

/**
 * Decoders for the single-byte ESC/POS status replies (`DLE EOT n` and `GS r 1`).
 *
 * Bit meanings follow the ESC/POS specification. On the reference unit only the idle
 * values were observed (`1e 1a 12 12` for `DLE EOT 1..4`, `00` for `GS r 1`), so treat the
 * fault bits as spec-derived until the hardware checklist confirms them.
 */
public object EscPosStatus {
    private const val BIT_2 = 0x04
    private const val BIT_3 = 0x08
    private const val BIT_5 = 0x20
    private const val BIT_6 = 0x40
    private const val GS_R_PAPER_END_BITS = 0x0C

    /** `DLE EOT 2` (offline cause) bit 2: the cover is open. */
    public fun coverOpen(offlineCause: Byte): Boolean = offlineCause.hasAny(BIT_2)

    /** `DLE EOT 3` (error status): cutter (bit 3), unrecoverable (bit 5) or auto-recoverable (bit 6) error. */
    public fun hasError(errorStatus: Byte): Boolean = errorStatus.hasAny(BIT_3 or BIT_5 or BIT_6)

    /** `DLE EOT 4` (paper sensor) bits 5–6: paper end. */
    public fun paperOut(paperSensor: Byte): Boolean = paperSensor.hasAny(BIT_5 or BIT_6)

    /** `DLE EOT 4` bits 2–3: paper near end. */
    public fun paperNearEnd(paperSensor: Byte): Boolean = paperSensor.hasAny(BIT_2 or BIT_3)

    /** `GS r 1` reply bits 2–3: paper end. `00` = paper present. */
    public fun paperOutFromGsR(paperStatus: Byte): Boolean = paperStatus.hasAny(GS_R_PAPER_END_BITS)

    private fun Byte.hasAny(mask: Int): Boolean = (toInt() and mask) != 0
}

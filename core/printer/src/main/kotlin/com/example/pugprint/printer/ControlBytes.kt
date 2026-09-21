package com.example.pugprint.printer

/** ASCII control bytes that prefix the ESC/POS commands (docs/PRINTER_PROTOCOL.md). */
internal object ControlBytes {
    const val LF: Byte = 0x0A
    const val DLE: Byte = 0x10
    const val EOT: Byte = 0x04
    const val ESC: Byte = 0x1B
    const val GS: Byte = 0x1D
    const val RS: Byte = 0x1E

    /** Every single-byte parameter is sent as an unsigned octet. */
    const val MAX_BYTE: Int = 0xFF
}

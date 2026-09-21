package com.example.pugprint.printer

import com.example.pugprint.printer.ControlBytes.DLE
import com.example.pugprint.printer.ControlBytes.EOT
import com.example.pugprint.printer.ControlBytes.GS
import com.example.pugprint.printer.ControlBytes.RS

/**
 * Query commands (docs/PRINTER_PROTOCOL.md § Query / status commands). The printer answers
 * on the TX characteristic; decode the payload with [PrinterReply.parse].
 */
public object PrinterQueries {
    private const val GS_G: Byte = 0x67
    private const val GS_R: Byte = 0x72
    private const val RS_G: Byte = 0x47
    private const val STATUS_ARG: Byte = 0x03
    private const val SERIAL_ARG: Byte = 0x39
    private const val PRODUCT_ARG: Byte = 0x69
    private const val PAPER_ARG: Byte = 0x01
    private val realtimeStatusKinds = 1..4

    /** `RS G 3` — replies `HV=…,SV=…,VOLT=…mv,DPI=…,` (see [PrinterReply.Status]). */
    public fun queryStatus(): ByteArray = byteArrayOf(RS, RS_G, STATUS_ARG)

    /** `GS g 0x39` — replies `sn:<16 chars>.` (see [PrinterReply.Serial]). */
    public fun querySerial(): ByteArray = byteArrayOf(GS, GS_G, SERIAL_ARG)

    /** `GS g 0x69` — replies `public id:0801.` (see [PrinterReply.Product]). */
    public fun queryProduct(): ByteArray = byteArrayOf(GS, GS_G, PRODUCT_ARG)

    /** `DLE EOT n` (n = 1..4) — standard ESC/POS real-time status, one reply byte. */
    public fun realtimeStatus(kind: Int): ByteArray {
        require(kind in realtimeStatusKinds) { "DLE EOT kind must be in $realtimeStatusKinds, got $kind" }
        return byteArrayOf(DLE, EOT, kind.toByte())
    }

    /** `GS r 1` — paper sensor status, one reply byte (`00` = paper present). */
    public fun queryPaper(): ByteArray = byteArrayOf(GS, GS_R, PAPER_ARG)
}

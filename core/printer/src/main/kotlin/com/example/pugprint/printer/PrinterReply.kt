package com.example.pugprint.printer

/**
 * A decoded notification from the printer's TX characteristic
 * (docs/PRINTER_PROTOCOL.md § Query / status commands). Text replies are ASCII, usually
 * NUL-terminated and often ending in `.`; both are stripped before matching.
 *
 * Single-byte ESC/POS replies (`DLE EOT n`, `GS r 1`) are not framed and cannot be told
 * apart from text by content — decode those with [EscPosStatus] instead.
 */
public sealed interface PrinterReply {
    /** Reply to [PrinterQueries.queryStatus]: `HV=H1.0,SV=V1.01,VOLT=7540mv,DPI=384,`. */
    public data class Status(
        /** e.g. `H1.0`. */
        val hardwareVersion: String,
        /** e.g. `V1.01`. */
        val firmwareVersion: String,
        /** Battery voltage from `VOLT=…mv`, or `null` when missing or unparseable. */
        val batteryMillivolts: Int?,
        /** Head width from `DPI=…`, or `null` when missing or unparseable. */
        val dotsPerLine: Int?,
    ) : PrinterReply {
        /**
         * The vendor app's numeric firmware version: all digits of [firmwareVersion]
         * concatenated (`V1.01` → 101), compared against [DensityProfile.NEW_FIRMWARE_THRESHOLD].
         */
        val firmwareNumber: Int = firmwareVersion.filter(Char::isDigit).toIntOrNull() ?: 0
    }

    /** Reply to [PrinterQueries.querySerial]: `sn:HBHW2500xxxxxxxx.`. */
    public data class Serial(
        val serialNumber: String,
    ) : PrinterReply {
        /** Last four characters, which the printer also uses as its `HB-nnnn` advertised name suffix. */
        val nameSuffix: String = serialNumber.takeLast(NAME_SUFFIX_LENGTH)
    }

    /** Reply to [PrinterQueries.queryProduct]: `public id:0801.`. */
    public data class Product(
        val productId: Int,
    ) : PrinterReply {
        val factory: FactoryType = FactoryType.fromProductId(productId)
    }

    /** Unsolicited `err:<code>.` notification. */
    public data class Error(
        val code: Int,
    ) : PrinterReply {
        val kind: ErrorKind =
            when (code) {
                ERROR_CLEARED -> ErrorKind.CLEARED
                ERROR_LID_OR_PAPER -> ErrorKind.LID_OR_PAPER
                else -> ErrorKind.OTHER
            }
    }

    /** Unsolicited `LABELOK` label-sensor event. Not an acknowledgement; ignore for flow control. */
    public data object LabelOk : PrinterReply

    /** Anything else, kept verbatim for logging. */
    public class Unknown(
        public val bytes: ByteArray,
    ) : PrinterReply {
        override fun toString(): String = "Unknown(${bytes.joinToString(" ") { "%02x".format(it) }})"
    }

    public enum class ErrorKind {
        CLEARED,

        /**
         * `err:\u0002`: the vendor app calls this "cover open", but the reference unit also sends
         * it with the lid closed and no paper loaded (hardware check 2026-09-21). Treat as
         * "lid open or out of paper"; the two cannot be told apart from this notification.
         */
        LID_OR_PAPER,
        OTHER,
    }

    public companion object {
        public const val ERROR_CLEARED: Int = 0x00
        public const val ERROR_LID_OR_PAPER: Int = 0x02
        private const val NAME_SUFFIX_LENGTH = 4
        private const val ERROR_PREFIX = "err:"
        private const val SERIAL_PREFIX = "sn:"
        private const val PRODUCT_KEY = "id:"
        private const val STATUS_KEY = "SV="
        private const val LABEL_OK = "LABELOK"
        private const val NUL = 0.toByte()

        /** Decodes one notification [payload]. Never throws; unrecognised input becomes [Unknown]. */
        public fun parse(payload: ByteArray): PrinterReply {
            if (payload.startsWith(ERROR_PREFIX) && payload.size > ERROR_PREFIX.length) {
                return Error(payload[ERROR_PREFIX.length].toInt() and ControlBytes.MAX_BYTE)
            }
            val text = String(payload, Charsets.ISO_8859_1).trimEnd(NUL.toInt().toChar()).trimEnd('.').trim()
            return when {
                text == LABEL_OK -> LabelOk
                text.startsWith(SERIAL_PREFIX) -> Serial(text.removePrefix(SERIAL_PREFIX).trim())
                text.contains(STATUS_KEY) -> parseStatus(text)
                text.contains(PRODUCT_KEY) -> parseProduct(text) ?: Unknown(payload)
                else -> Unknown(payload)
            }
        }

        private fun parseStatus(text: String): Status {
            val fields =
                text
                    .split(',')
                    .mapNotNull { field ->
                        val eq = field.indexOf('=')
                        if (eq < 0) null else field.substring(0, eq).trim() to field.substring(eq + 1).trim()
                    }.toMap()
            return Status(
                hardwareVersion = fields["HV"].orEmpty(),
                firmwareVersion = fields["SV"].orEmpty(),
                batteryMillivolts = fields["VOLT"]?.leadingInt(),
                dotsPerLine = fields["DPI"]?.leadingInt(),
            )
        }

        private fun parseProduct(text: String): Product? = text.substringAfter(PRODUCT_KEY).leadingInt()?.let(::Product)

        private fun String.leadingInt(): Int? = trim().takeWhile(Char::isDigit).toIntOrNull()

        private fun ByteArray.startsWith(prefix: String): Boolean =
            size >= prefix.length && prefix.indices.all { this[it] == prefix[it].code.toByte() }
    }
}

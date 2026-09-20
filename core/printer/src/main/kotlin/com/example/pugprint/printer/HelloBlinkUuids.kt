package com.example.pugprint.printer

/**
 * GATT identifiers for the Hello Blink's Microchip transparent-UART service.
 * See docs/PRINTER_PROTOCOL.md § Transports. The printer is discovered at runtime by
 * [SERVICE]; never by MAC or advertised name.
 */
public object HelloBlinkUuids {
    /** Transparent-UART service advertised by the printer. */
    public const val SERVICE: String = "49535343-fe7d-4ae5-8fa9-9fafd205e455"

    /** Host → printer characteristic (write-without-response). */
    public const val RX: String = "49535343-8841-43f4-a8d4-ecbe34729bb3"

    /** Printer → host characteristic (notify; enable via CCCD 0x2902). */
    public const val TX: String = "49535343-1e4d-4bd9-ba61-23c647249616"
}

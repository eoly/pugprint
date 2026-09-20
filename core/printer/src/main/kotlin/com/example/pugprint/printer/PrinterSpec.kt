package com.example.pugprint.printer

/** Physical constants of the Hello Blink print head (docs/PRINTER_PROTOCOL.md § Hardware). */
public object PrinterSpec {
    /** Dots per raster row across the 58 mm head. */
    public const val DOTS_PER_LINE: Int = 384

    /** Packed 1bpp bytes per raster row (`xL` of `GS v 0`). */
    public const val BYTES_PER_ROW: Int = DOTS_PER_LINE / 8

    /** Head resolution, dots per millimetre (203 dpi). */
    public const val DOTS_PER_MM: Int = 8
}

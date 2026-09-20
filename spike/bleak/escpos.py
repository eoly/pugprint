"""Minimal ESC/POS encoder for the Hello Blink spike (confirmed family, 2026-09-20).

Transport: Microchip transparent UART over BLE.
  service  49535343-fe7d-4ae5-8fa9-9fafd205e455
  RX (host->printer) 49535343-8841-43f4-a8d4-ecbe34729bb3  write-without-response
  TX (printer->host) 49535343-1e4d-4bd9-ba61-23c647249616  notify

Raster: GS v 0  ->  1d 76 30 m xL xH yL yH data
  m = 0 (normal), x = bytes per row (48 for 384 dots), y = rows.
  1 bpp, MSB-first within each byte, 1 = black.  (VERIFY on paper with the stripe pattern.)
"""

from __future__ import annotations

SERVICE_UUID = "49535343-fe7d-4ae5-8fa9-9fafd205e455"
UART_RX = "49535343-8841-43f4-a8d4-ecbe34729bb3"
UART_TX = "49535343-1e4d-4bd9-ba61-23c647249616"

ROW_DOTS = 384
ROW_BYTES = ROW_DOTS // 8

ESC, GS, DLE, EOT = 0x1B, 0x1D, 0x10, 0x04


def init() -> bytes:
    return bytes([ESC, 0x40])  # ESC @


def text(s: str) -> bytes:
    return s.encode("ascii", "replace")


def line_feed(n: int = 1) -> bytes:
    return bytes([ESC, 0x64, n])  # ESC d n : print and feed n lines


def feed_dots(n: int) -> bytes:
    return bytes([ESC, 0x4A, n])  # ESC J n : print and feed n motion units


def status_query(n: int) -> bytes:
    return bytes([DLE, EOT, n])  # DLE EOT n


def paper_status() -> bytes:
    return bytes([GS, 0x72, 0x01])  # GS r 1


def raster(rows: list[bytes], mode: int = 0) -> bytes:
    """GS v 0 — one raster block for all rows. Every row must be ROW_BYTES long."""
    for r in rows:
        if len(r) != ROW_BYTES:
            raise ValueError(f"row must be {ROW_BYTES} bytes, got {len(r)}")
    y = len(rows)
    return bytes([GS, 0x76, 0x30, mode, ROW_BYTES & 0xFF, ROW_BYTES >> 8, y & 0xFF, y >> 8]) + b"".join(rows)


def raster_chunks(rows: list[bytes], rows_per_block: int = 64) -> bytes:
    """Same image as several GS v 0 blocks — friendlier to small printer buffers."""
    return b"".join(raster(rows[i : i + rows_per_block]) for i in range(0, len(rows), rows_per_block))


LINE_SPACING_24 = b"\x1b\x33\x18"  # ESC 3 24 — bands butt together with no gap
LINE_SPACING_DEFAULT = b"\x1b\x32"  # ESC 2
ACK = b"LABELOK"  # printer pushes this on the TX characteristic after each committed band


def bit_image_band_list(rows: list[list[bool]], m: int = 33) -> list[bytes]:
    """ESC * column format, one entry per 24-dot-tall band: `ESC * m nL nH data` + LF.
    m=33 → 24 dots/column (3 bytes, MSB = top dot), double density (8 dots/mm, 384 wide).
    Send each band and wait for ACK before the next — the printer has no flow control and
    silently drops everything if its input buffer overflows (seen at 2 bands / 2.3 KB sent
    at 245 B per 20 ms)."""
    h = len(rows)
    bands: list[bytes] = []
    for band in range(0, h, 24):
        n = ROW_DOTS
        out = bytearray(bytes([ESC, 0x2A, m, n & 0xFF, n >> 8]))
        for x in range(n):
            v = 0
            for k in range(24):
                y = band + k
                if y < h and rows[y][x]:
                    v |= 1 << (23 - k)
            out += bytes([(v >> 16) & 0xFF, (v >> 8) & 0xFF, v & 0xFF])
        out += b"\x0a"
        bands.append(bytes(out))
    return bands


def bit_image_band_list_8dot(rows: list[list[bool]], m: int = 1) -> list[bytes]:
    """ESC * with 8-dot columns (m=0 single density, m=1 double density): 1 byte per
    column, MSB = top dot, 8-row bands. Used to test whether the head only implements
    the 8-dot modes."""
    h = len(rows)
    bands: list[bytes] = []
    for band in range(0, h, 8):
        n = ROW_DOTS
        out = bytearray(bytes([ESC, 0x2A, m, n & 0xFF, n >> 8]))
        for x in range(n):
            v = 0
            for k in range(8):
                y = band + k
                if y < h and rows[y][x]:
                    v |= 1 << (7 - k)
            out.append(v)
        out += b"\x0a"
        bands.append(bytes(out))
    return bands


def bit_image_bands(rows: list[list[bool]], m: int = 33) -> bytes:
    """Whole image as one byte string (fixtures / dumps). For live sending prefer
    bit_image_band_list + ack-gated writes."""
    return LINE_SPACING_24 + b"".join(bit_image_band_list(rows, m)) + LINE_SPACING_DEFAULT


def pack_row(pixels: list[bool]) -> bytes:
    """384 booleans (True = black) -> 48 bytes, MSB-first (ESC/POS raster convention)."""
    if len(pixels) != ROW_DOTS:
        raise ValueError(f"row must be {ROW_DOTS} pixels, got {len(pixels)}")
    out = bytearray(ROW_BYTES)
    for i, black in enumerate(pixels):
        if black:
            out[i >> 3] |= 0x80 >> (i & 7)
    return bytes(out)


def hexdump(b: bytes) -> str:
    return " ".join(f"{x:02x}" for x in b)

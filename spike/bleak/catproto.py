"""Cat-printer family frame encoder for the Phase 0 spike.

Every constant here mirrors the ASSUMED table in docs/PRINTER_PROTOCOL.md.
As the spike confirms or refutes each one, update it here AND in the doc.

Frame:  51 78 | CMD | DIR | LEN_LO LEN_HI | PAYLOAD... | CRC8(payload) | FF
"""

from __future__ import annotations

# --- GATT ------------------------------------------------------------------
SERVICE_UUID = "0000ae30-0000-1000-8000-00805f9b34fb"
WRITE_UUID = "0000ae01-0000-1000-8000-00805f9b34fb"  # write-no-response
NOTIFY_UUID = "0000ae02-0000-1000-8000-00805f9b34fb"

# --- Commands (ASSUMED) ----------------------------------------------------
CMD_FEED_PAPER = 0xA1
CMD_PRINT_ROW = 0xA2
CMD_GET_STATE = 0xA3
CMD_SET_QUALITY = 0xA4
CMD_GET_INFO = 0xA8
CMD_SET_ENERGY = 0xAF
CMD_DRAW_MODE = 0xBE
CMD_PRINT_ROW_RLE = 0xBF

MAGIC = b"\x51\x78"
DIR_HOST_TO_PRINTER = 0x00
FRAME_END = 0xFF
ROW_DOTS = 384
ROW_BYTES = ROW_DOTS // 8

# --- CRC8, poly 0x07, init 0, no reflect (ASSUMED) -------------------------
_CRC_TABLE = []
for _i in range(256):
    _c = _i
    for _ in range(8):
        _c = ((_c << 1) ^ 0x07) & 0xFF if _c & 0x80 else (_c << 1) & 0xFF
    _CRC_TABLE.append(_c)


def crc8(data: bytes) -> int:
    c = 0
    for b in data:
        c = _CRC_TABLE[c ^ b]
    return c


def frame(cmd: int, payload: bytes) -> bytes:
    if len(payload) > 0xFFFF:
        raise ValueError("payload too long")
    return (
        MAGIC
        + bytes([cmd, DIR_HOST_TO_PRINTER, len(payload) & 0xFF, len(payload) >> 8])
        + payload
        + bytes([crc8(payload), FRAME_END])
    )


# --- Convenience builders --------------------------------------------------
def get_state() -> bytes:
    return frame(CMD_GET_STATE, b"\x00")


def get_info() -> bytes:
    return frame(CMD_GET_INFO, b"\x00")


def set_quality(level: int = 0x33) -> bytes:
    """Family default seen in rbaron/catprinter is 0x33 for images. VERIFY."""
    return frame(CMD_SET_QUALITY, bytes([level]))


def set_energy(value: int = 0x2EE0) -> bytes:
    """u16 little-endian. 0x2EE0 (12000) is a common family default. VERIFY."""
    return frame(CMD_SET_ENERGY, bytes([value & 0xFF, value >> 8]))


def draw_mode(image: bool = True) -> bytes:
    """0x00 = image (dither), 0x01 = text (threshold). VERIFY."""
    return frame(CMD_DRAW_MODE, bytes([0x00 if image else 0x01]))


def feed(lines: int = 0x30) -> bytes:
    return frame(CMD_FEED_PAPER, bytes([lines & 0xFF, lines >> 8]))


def print_row(packed_row: bytes) -> bytes:
    if len(packed_row) != ROW_BYTES:
        raise ValueError(f"row must be {ROW_BYTES} bytes, got {len(packed_row)}")
    return frame(CMD_PRINT_ROW, packed_row)


def pack_row(pixels: list[bool]) -> bytes:
    """Pack 384 booleans (True = black) into 48 bytes, LSB-first within each byte (ASSUMED)."""
    if len(pixels) != ROW_DOTS:
        raise ValueError(f"row must be {ROW_DOTS} pixels, got {len(pixels)}")
    out = bytearray(ROW_BYTES)
    for i, black in enumerate(pixels):
        if black:
            out[i >> 3] |= 1 << (i & 7)
    return bytes(out)


def print_job(rows: list[bytes], *, energy: int | None = None) -> list[bytes]:
    """Full frame sequence for one print. Order is ASSUMED; confirm from the HCI capture."""
    seq = [get_state(), set_quality(), draw_mode(True)]
    if energy is not None:
        seq.append(set_energy(energy))
    seq.extend(print_row(r) for r in rows)
    seq.append(feed())
    return seq


def hexdump(b: bytes) -> str:
    return " ".join(f"{x:02x}" for x in b)

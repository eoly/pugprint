"""Print a test pattern or image to the Hello Blink via ESC/POS raster, or dump fixtures.

Usage:
  python spike/bleak/escpos_print.py <uuid> black              # 24 all-black rows
  python spike/bleak/escpos_print.py <uuid> stripe             # bit-order / width test
  python spike/bleak/escpos_print.py <uuid> text               # "PUGPRINT" text line only
  python spike/bleak/escpos_print.py <uuid> photo.png          # any image, scaled to 384 wide
  python spike/bleak/escpos_print.py --dump stripe             # write fixtures, no BLE

Pacing: --chunk (default MTU-3) and --delay (seconds between chunks).
"""

from __future__ import annotations

import argparse
import asyncio
from pathlib import Path

from escpos import (
    ACK,
    LINE_SPACING_24,
    LINE_SPACING_DEFAULT,
    ROW_DOTS,
    UART_RX,
    UART_TX,
    bit_image_band_list,
    bit_image_band_list_8dot,
    hexdump,
    init,
    line_feed,
    pack_row,
    raster_chunks,
    text,
)

FIXTURES = Path(__file__).resolve().parent.parent / "fixtures" / "print_job"


def pattern_black(rows: int = 24) -> list[list[bool]]:
    return [[True] * ROW_DOTS for _ in range(rows)]


def pattern_stripe(rows: int = 24) -> list[list[bool]]:
    """Leftmost 16 px black; single black px at x=100, 200, 300, 383.
    Confirms bit order (MSB-first?), left/right orientation, and full 384-dot width."""
    row = [False] * ROW_DOTS
    for x in range(16):
        row[x] = True
    for x in (100, 200, 300, 383):
        row[x] = True
    return [list(row) for _ in range(rows)]


def pattern_widths(rows: int = 48) -> list[list[bool]]:
    """Left 16-px block; vertical lines 1,2,3,4,6,8,12 px wide at 48-px spacing;
    8-px block at the far right (x=376..383). Finds the minimum printable line width
    at default energy and confirms the 384-dot width."""
    row = [False] * ROW_DOTS
    for x in range(16):
        row[x] = True
    x = 48
    for w in (1, 2, 3, 4, 6, 8, 12):
        for k in range(w):
            row[x + k] = True
        x += 48
    for x in range(376, 384):
        row[x] = True
    return [list(row) for _ in range(rows)]


def pattern_from_image(path: Path) -> list[list[bool]]:
    from PIL import Image

    img = Image.open(path).convert("L")
    h = max(1, round(img.height * ROW_DOTS / img.width))
    img = img.resize((ROW_DOTS, h)).convert("1")  # Floyd–Steinberg
    px = img.load()
    return [[px[x, y] == 0 for x in range(ROW_DOTS)] for y in range(h)]


def to_pbm(rows: list[list[bool]]) -> str:
    body = "\n".join(" ".join("1" if b else "0" for b in r) for r in rows)
    return f"P1\n{ROW_DOTS} {len(rows)}\n{body}\n"


def build(name: str, encoding: str, nrows: int | None = None) -> tuple[str, list[bytes], list[list[bool]] | None]:
    """Returns (stem, parts, rows). Each part ending in LF is a commit point: in ack mode
    the sender waits for LABELOK after it before sending the next part."""
    if name == "text":
        return "text_line", [init() + text("PUGPRINT ESC/POS OK\n"), line_feed(3)], None
    if name == "black":
        rows = pattern_black(nrows or 24)
        stem = f"black_{len(rows)}_rows"
    elif name == "stripe":
        rows = pattern_stripe(nrows or 24)
        stem = f"stripe_{len(rows)}_rows"
    elif name == "widths":
        rows = pattern_widths(nrows or 48)
        stem = f"widths_{len(rows)}_rows"
    else:
        p = Path(name)
        rows = pattern_from_image(p)
        stem = p.stem
    if encoding == "raster":
        parts = [init() + raster_chunks([pack_row(r) for r in rows]) + line_feed(3)]
    elif encoding == "column8":
        bands = bit_image_band_list_8dot(rows)
        parts = [init() + b"\x1b\x33\x08" + bands[0], *bands[1:], LINE_SPACING_DEFAULT + line_feed(3)]
    else:
        bands = bit_image_band_list(rows)
        parts = [init() + LINE_SPACING_24 + bands[0], *bands[1:], LINE_SPACING_DEFAULT + line_feed(3)]
    return f"{stem}.{encoding}", parts, rows


async def send(address: str, parts: list[bytes], chunk: int | None, delay: float, ack: bool, ack_timeout: float) -> None:
    from bleak import BleakClient

    acks: asyncio.Queue[bytes] = asyncio.Queue()

    def on_tx(_c, d: bytearray) -> None:
        print(f"  <- {hexdump(bytes(d))}  {bytes(d)!r}")
        acks.put_nowait(bytes(d))

    total = sum(len(p) for p in parts)
    print(f"Connecting to {address} ...")
    async with BleakClient(address, timeout=20.0) as client:
        size = chunk or (client.mtu_size - 3)
        print(f"Connected. MTU={client.mtu_size}, chunk={size}, delay={delay}s, ack={'on' if ack else 'off'}, "
              f"{len(parts)} parts, {total}B")
        await client.start_notify(UART_TX, on_tx)
        n = 0
        for i, part in enumerate(parts, 1):
            for off in range(0, len(part), size):
                await client.write_gatt_char(UART_RX, part[off : off + size], response=False)
                n += 1
                await asyncio.sleep(delay)
            if ack and part.endswith(b"\x0a"):
                try:
                    got = await asyncio.wait_for(acks.get(), timeout=ack_timeout)
                    if got != ACK:
                        print(f"  ! part {i}: unexpected reply {got!r}")
                except asyncio.TimeoutError:
                    print(f"  ! part {i}: no {ACK!r} within {ack_timeout}s — continuing")
        print(f"Sent {n} chunks. Waiting for printer ...")
        await asyncio.sleep(3.0)
        await client.stop_notify(UART_TX)
    print("Done.")


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("address", nargs="?", help="device UUID from scan.py (omit with --dump)")
    ap.add_argument("pattern", help="black | stripe | text | path/to/image")
    ap.add_argument("--dump", action="store_true", help="write fixtures instead of printing")
    ap.add_argument("--encoding", choices=["column", "column8", "raster"], default="column",
                    help="ESC * 24-dot column (m=33), ESC * 8-dot column (m=1), or GS v 0 raster")
    ap.add_argument("--chunk", type=int, default=None)
    ap.add_argument("--delay", type=float, default=0.02)
    ap.add_argument("--rows", type=int, default=None, help="pattern height in dots (black/stripe/widths)")
    ap.add_argument("--ack", action="store_true", help="wait for LABELOK between bands (found NOT to be an ack)")
    ap.add_argument("--ack-timeout", type=float, default=5.0)
    a = ap.parse_args()

    stem, parts, rows = build(a.pattern, a.encoding, a.rows)
    if a.dump:
        FIXTURES.mkdir(parents=True, exist_ok=True)
        if rows is not None:
            (FIXTURES / f"{stem}.pbm").write_text(to_pbm(rows))
        (FIXTURES / f"{stem}.escpos.hex").write_text("\n".join(hexdump(p) for p in parts) + "\n")
        print(f"Wrote {FIXTURES / stem}.escpos.hex ({len(parts)} parts, {sum(map(len, parts))} bytes)")
        return
    if not a.address:
        ap.error("address is required unless --dump is given")
    asyncio.run(send(a.address, parts, a.chunk, a.delay, a.ack, a.ack_timeout))


if __name__ == "__main__":
    main()

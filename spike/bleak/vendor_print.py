"""Print using the EXACT sequence the official HelloBlink app sends (from jadx of
com.yhk.rabbit.print.task.TaskPosPrint + utils.ImageUtil.printDraw2):

  1d 49 f0 <density>        GS I 0xF0 n   density (new fw: 12/15/18 light/med/dark)
  [1d 49 f1 <speed>]        GS I 0xF1 n   speed, only sent to old firmware (SV < 119)
  [1d 49 f8 <copies>]       only if copies > 1
  1b 40                     ESC @
  1d 76 30 00 30 00 yL yH + rows   GS v 0 raster, ONE block for the whole image,
                                    48 bytes/row (384 dots), MSB-first, 1 = black
  (sleep 250 ms)
  0a 0a 0a 0a               feed

Usage:
  python spike/bleak/vendor_print.py <uuid> black [--rows 48] [--density 15] [--delay 0.05]
  python spike/bleak/vendor_print.py <uuid> stripe | widths | photo.png
  python spike/bleak/vendor_print.py --dump black
"""
from __future__ import annotations

import argparse
import asyncio
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from escpos import ROW_BYTES, ROW_DOTS, UART_RX, UART_TX, hexdump, pack_row  # noqa: E402
from escpos_print import pattern_black, pattern_from_image, pattern_stripe, pattern_widths, to_pbm  # noqa: E402

FIXTURES = Path(__file__).resolve().parent.parent / "fixtures" / "print_job"


def density(n: int) -> bytes:
    return bytes([0x1D, 0x49, 0xF0, n & 0xFF])


def speed(n: int) -> bytes:
    return bytes([0x1D, 0x49, 0xF1, n & 0xFF])


def raster_block(rows: list[bytes]) -> bytes:
    y = len(rows)
    return bytes([0x1D, 0x76, 0x30, 0x00, ROW_BYTES, 0x00, y & 0xFF, y >> 8]) + b"".join(rows)


def raster_blocks(rows: list[bytes], block_rows: int) -> list[bytes]:
    """Split the image into several GS v 0 blocks of block_rows rows each (0 = one block)."""
    if block_rows <= 0:
        return [raster_block(rows)]
    return [raster_block(rows[i : i + block_rows]) for i in range(0, len(rows), block_rows)]


def build(name: str, nrows: int) -> tuple[str, list[list[bool]]]:
    if name == "black":
        return f"black_{nrows}_rows", pattern_black(nrows)
    if name == "stripe":
        return f"stripe_{nrows}_rows", pattern_stripe(nrows)
    if name == "widths":
        return f"widths_{nrows}_rows", pattern_widths(nrows)
    p = Path(name)
    return p.stem, pattern_from_image(p)


async def send(address: str, parts: list[tuple[str, bytes, float]], chunk: int | None, delay: float) -> None:
    from bleak import BleakClient

    async with BleakClient(address, timeout=20.0) as client:
        size = chunk or (client.mtu_size - 3)
        print(f"Connected. MTU={client.mtu_size} chunk={size} delay={delay}s")
        await client.start_notify(UART_TX, lambda _c, d: print(f"  <- {hexdump(bytes(d))}  {bytes(d)!r}"))
        for label, data, pause in parts:
            print(f"  -> {label}: {len(data)}B" + (f"  {hexdump(data)}" if len(data) <= 16 else ""))
            for off in range(0, len(data), size):
                await client.write_gatt_char(UART_RX, data[off : off + size], response=False)
                await asyncio.sleep(delay)
            await asyncio.sleep(pause)
        print("Waiting for printer ...")
        await asyncio.sleep(3.0)
        await client.stop_notify(UART_TX)
    print("Done.")


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("address", nargs="?")
    ap.add_argument("pattern")
    ap.add_argument("--rows", type=int, default=48)
    ap.add_argument("--density", type=int, default=15, help="GS I F0 n; app uses 12/15/18 on new fw")
    ap.add_argument("--speed", type=int, default=None, help="GS I F1 n; app sends 0x1e only to old fw")
    ap.add_argument("--chunk", type=int, default=None)
    ap.add_argument("--delay", type=float, default=0.05, help="pause between BLE chunks")
    ap.add_argument("--block-rows", type=int, default=0, help="rows per GS v 0 block (0 = whole image)")
    ap.add_argument("--block-pause", type=float, default=0.0, help="pause after each block")
    ap.add_argument("--dump", action="store_true")
    a = ap.parse_args()

    stem, rows = build(a.pattern, a.rows)
    packed = [pack_row(r) for r in rows]
    parts: list[tuple[str, bytes, float]] = [("density", density(a.density), 0.0)]
    if a.speed is not None:
        parts.append(("speed", speed(a.speed), 0.0))
    parts.append(("init", b"\x1b\x40", 0.0))
    blocks = raster_blocks(packed, a.block_rows)
    for i, blk in enumerate(blocks):
        parts.append((f"raster block {i + 1}/{len(blocks)}", blk, 0.25 if i == len(blocks) - 1 else a.block_pause))
    parts.append(("feed", b"\x0a\x0a\x0a\x0a", 0.0))
    if a.dump:
        FIXTURES.mkdir(parents=True, exist_ok=True)
        (FIXTURES / f"{stem}.pbm").write_text(to_pbm(rows))
        (FIXTURES / f"{stem}.vendor.hex").write_text("\n".join(f"# {l}\n{hexdump(d)}" for l, d, _ in parts) + "\n")
        print(f"Wrote {FIXTURES / stem}.vendor.hex")
        return
    if not a.address:
        ap.error("address required unless --dump")
    asyncio.run(send(a.address, parts, a.chunk, a.delay))


if __name__ == "__main__":
    main()

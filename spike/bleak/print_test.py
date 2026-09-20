"""Print a test bitmap, or dump the frames it would send as golden fixtures.

Usage:
  python spike/bleak/print_test.py <uuid> black            # 8 all-black rows
  python spike/bleak/print_test.py <uuid> stripe           # left edge + 1px stripe, 8 rows
  python spike/bleak/print_test.py <uuid> photo.png        # any image, scaled to 384 wide
  python spike/bleak/print_test.py --dump black            # no BLE; write fixtures/print_job/black_8_rows.*

Pacing knobs: --chunk (bytes per write, default MTU-3) and --delay (seconds between chunks).
"""

from __future__ import annotations

import argparse
import asyncio
from pathlib import Path

from catproto import ROW_DOTS, WRITE_UUID, NOTIFY_UUID, hexdump, pack_row, print_job

FIXTURES = Path(__file__).resolve().parent.parent / "fixtures" / "print_job"


# --- test patterns ---------------------------------------------------------
def pattern_black(rows: int = 8) -> list[list[bool]]:
    return [[True] * ROW_DOTS for _ in range(rows)]


def pattern_stripe(rows: int = 8) -> list[list[bool]]:
    """Leftmost 16 px black, then a single black px at x=100, x=200, x=300, and x=383.
    Confirms bit order, left/right orientation, and full 384-dot width."""
    row = [False] * ROW_DOTS
    for x in range(16):
        row[x] = True
    for x in (100, 200, 300, 383):
        row[x] = True
    return [list(row) for _ in range(rows)]


def pattern_from_image(path: Path) -> list[list[bool]]:
    from PIL import Image

    img = Image.open(path).convert("L")
    h = max(1, round(img.height * ROW_DOTS / img.width))
    img = img.resize((ROW_DOTS, h)).convert("1")  # PIL default = Floyd-Steinberg
    px = img.load()
    return [[px[x, y] == 0 for x in range(ROW_DOTS)] for y in range(h)]


def to_pbm(rows: list[list[bool]]) -> str:
    body = "\n".join(" ".join("1" if b else "0" for b in r) for r in rows)
    return f"P1\n{ROW_DOTS} {len(rows)}\n{body}\n"


# --- main ------------------------------------------------------------------
def build(name: str) -> tuple[str, list[list[bool]]]:
    if name == "black":
        return "black_8_rows", pattern_black()
    if name == "stripe":
        return "stripe_8_rows", pattern_stripe()
    p = Path(name)
    return p.stem, pattern_from_image(p)


async def send(address: str, frames: list[bytes], chunk: int | None, delay: float) -> None:
    from bleak import BleakClient

    stream = b"".join(frames)
    print(f"Connecting to {address} ...")
    async with BleakClient(address, timeout=20.0) as client:
        size = chunk or (client.mtu_size - 3)
        print(f"Connected. MTU={client.mtu_size}, chunk={size}, delay={delay}s, total={len(stream)}B")
        await client.start_notify(NOTIFY_UUID, lambda _c, d: print(f"  <- {hexdump(bytes(d))}"))
        n = 0
        for off in range(0, len(stream), size):
            await client.write_gatt_char(WRITE_UUID, stream[off : off + size], response=False)
            n += 1
            await asyncio.sleep(delay)
        print(f"Sent {n} chunks. Waiting for printer ...")
        await asyncio.sleep(3.0)
        await client.stop_notify(NOTIFY_UUID)
    print("Done.")


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("address", nargs="?", help="device UUID from scan.py (omit with --dump)")
    ap.add_argument("pattern", help="black | stripe | path/to/image")
    ap.add_argument("--dump", action="store_true", help="write fixtures instead of printing")
    ap.add_argument("--energy", type=lambda s: int(s, 0), default=None, help="e.g. 0x2EE0")
    ap.add_argument("--chunk", type=int, default=None, help="bytes per BLE write (default MTU-3)")
    ap.add_argument("--delay", type=float, default=0.02, help="seconds between chunks")
    a = ap.parse_args()

    name, rows = build(a.pattern)
    frames = print_job([pack_row(r) for r in rows], energy=a.energy)

    if a.dump:
        FIXTURES.mkdir(parents=True, exist_ok=True)
        (FIXTURES / f"{name}.pbm").write_text(to_pbm(rows))
        (FIXTURES / f"{name}.hex").write_text("\n".join(hexdump(f) for f in frames) + "\n")
        print(f"Wrote {FIXTURES / name}.pbm and .hex ({len(frames)} frames)")
        return

    if not a.address:
        ap.error("address is required unless --dump is given")
    asyncio.run(send(a.address, frames, a.chunk, a.delay))


if __name__ == "__main__":
    main()

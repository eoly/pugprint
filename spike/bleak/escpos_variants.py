"""Send several candidate image-print encodings, each a solid black bar, separated by feeds.
Whichever bar appears on paper identifies the working encoding.

Usage: python spike/bleak/escpos_variants.py <uuid> [--delay 0.02] [--only N]
"""

from __future__ import annotations

import argparse
import asyncio

from bleak import BleakClient

from escpos import ROW_BYTES, UART_RX, UART_TX, hexdump

BLACK_ROW = b"\xff" * ROW_BYTES
ROWS = 24


def v1_phomemo() -> bytes:
    """Phomemo M02-style: ESC @, ESC a 1, density 1F 11 02 n, GS v 0, then vendor footer."""
    y = ROWS
    return (
        b"\x1b\x40\x1b\x61\x01\x1f\x11\x02\x04"
        + b"\x1d\x76\x30\x00" + bytes([ROW_BYTES, 0, y & 0xFF, y >> 8]) + BLACK_ROW * y
        + b"\x1b\x64\x02\x1f\x11\x08\x1f\x11\x0e\x1f\x11\x07\x1f\x11\x09"
    )


def v2_esc_star() -> bytes:
    """ESC * m=33 (24-dot double density, column format): 384 columns x 3 bytes each."""
    n = 384
    return b"\x1b\x40" + b"\x1b\x33\x18" + b"\x1b\x2a\x21" + bytes([n & 0xFF, n >> 8]) + b"\xff" * (n * 3) + b"\x0a" + b"\x1b\x64\x02"


def v3_gs_paren_l() -> bytes:
    """GS ( L fn 112 (store raster) + fn 50 (print)."""
    y = ROWS
    data = BLACK_ROW * y
    p = 10 + len(data)
    hdr = b"\x1d\x28\x4c" + bytes([p & 0xFF, p >> 8]) + b"\x30\x70\x30\x01\x01\x31" + bytes([ROW_BYTES * 8 & 0xFF, (ROW_BYTES * 8) >> 8, y & 0xFF, y >> 8])
    print_cmd = b"\x1d\x28\x4c\x02\x00\x30\x32"
    return b"\x1b\x40" + hdr + data + print_cmd + b"\x1b\x64\x02"


def v4_gs_v0_density() -> bytes:
    """Plain GS v 0 again but with GS ( K / DC2 # density hints first, and 8-row blocks."""
    blocks = b"".join(b"\x1d\x76\x30\x00" + bytes([ROW_BYTES, 0, 8, 0]) + BLACK_ROW * 8 for _ in range(ROWS // 8))
    return b"\x1b\x40" + b"\x12\x23\x0f" + b"\x1d\x28\x4b\x02\x00\x31\x08" + blocks + b"\x1b\x64\x02"


def v5_gs_v0_lf() -> bytes:
    """Plain GS v 0, 48 rows (6 mm), followed by a bare LF to commit."""
    y = 48
    return b"\x1b\x40" + b"\x1d\x76\x30\x00" + bytes([ROW_BYTES, 0, y & 0xFF, y >> 8]) + BLACK_ROW * y + b"\x0a" + b"\x1b\x64\x02"


VARIANTS = [
    ("v1 phomemo-style", v1_phomemo),
    ("v2 ESC * column", v2_esc_star),
    ("v3 GS ( L", v3_gs_paren_l),
    ("v4 GS v 0 + density", v4_gs_v0_density),
    ("v5 GS v 0 48 rows + LF", v5_gs_v0_lf),
]


async def main(address: str, delay: float, only: list[int] | None) -> None:
    async with BleakClient(address, timeout=20.0) as client:
        size = client.mtu_size - 3
        print(f"Connected. MTU={client.mtu_size}")
        await client.start_notify(UART_TX, lambda _c, d: print(f"  <- {hexdump(bytes(d))}  {bytes(d)!r}"))
        for i, (name, fn) in enumerate(VARIANTS, 1):
            if only and i not in only:
                continue
            job = fn()
            print(f"\n[{i}] {name}: {len(job)} bytes")
            for off in range(0, len(job), size):
                await client.write_gatt_char(UART_RX, job[off : off + size], response=False)
                await asyncio.sleep(delay)
            await asyncio.sleep(4.0)
    print("Done.")


if __name__ == "__main__":
    p = argparse.ArgumentParser()
    p.add_argument("address")
    p.add_argument("--delay", type=float, default=0.02)
    p.add_argument("--only", type=int, nargs="+", default=None, help="variant numbers to send")
    a = p.parse_args()
    asyncio.run(main(a.address, a.delay, a.only))

"""Transparent-UART probe: subscribe to TX, send raw hex commands to RX, print replies.

Usage:
  python spike/bleak/uart_probe.py <uuid> "10 04 01" "10 04 02" ...
  python spike/bleak/uart_probe.py <uuid> --wait 2 "1b 40"
"""

import argparse
import asyncio

from bleak import BleakClient

UART_TX = "49535343-1e4d-4bd9-ba61-23c647249616"  # notify, printer -> host
UART_RX = "49535343-8841-43f4-a8d4-ecbe34729bb3"  # write-no-response, host -> printer
UART_CTL = "49535343-aca3-481c-91ec-d85e28a60318"  # notify+write, control point


def hexdump(b: bytes) -> str:
    return " ".join(f"{x:02x}" for x in b)


async def main(address: str, cmds: list[str], wait: float) -> None:
    print(f"Connecting to {address} ...")
    async with BleakClient(address, timeout=20.0) as client:
        print(f"Connected. MTU={client.mtu_size}")
        await client.start_notify(UART_TX, lambda _c, d: print(f"  <- TX  ({len(d)}B): {hexdump(bytes(d))}"))
        try:
            await client.start_notify(UART_CTL, lambda _c, d: print(f"  <- CTL ({len(d)}B): {hexdump(bytes(d))}"))
        except Exception as e:  # noqa: BLE001
            print(f"  (control-point notify unavailable: {e})")
        for c in cmds:
            pkt = bytes.fromhex(c.replace(" ", ""))
            print(f"\n  -> {hexdump(pkt)}")
            await client.write_gatt_char(UART_RX, pkt, response=False)
            await asyncio.sleep(wait)
        print(f"\nListening {wait}s more ...")
        await asyncio.sleep(wait)
    print("Disconnected.")


if __name__ == "__main__":
    p = argparse.ArgumentParser()
    p.add_argument("address")
    p.add_argument("cmds", nargs="+", help="hex byte strings")
    p.add_argument("--wait", type=float, default=1.5)
    a = p.parse_args()
    asyncio.run(main(a.address, a.cmds, a.wait))

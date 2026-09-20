"""Connect, dump the GATT table, subscribe to ae02, send A3 + A8, and print every reply.

Usage: python spike/bleak/probe.py <device-uuid-from-scan.py>
"""

import argparse
import asyncio

from bleak import BleakClient

from catproto import NOTIFY_UUID, WRITE_UUID, get_info, get_state, hexdump


def on_notify(_char, data: bytearray) -> None:
    print(f"  <- notify ({len(data)}B): {hexdump(bytes(data))}")


async def main(address: str, wait: float) -> None:
    print(f"Connecting to {address} ...")
    async with BleakClient(address, timeout=20.0) as client:
        print(f"Connected. MTU={client.mtu_size}")
        print("\nGATT table:")
        for svc in client.services:
            print(f"  service {svc.uuid}  ({svc.description})")
            for ch in svc.characteristics:
                props = ",".join(ch.properties)
                print(f"    char {ch.uuid}  handle={ch.handle}  [{props}]")
                for d in ch.descriptors:
                    print(f"      desc {d.uuid}  handle={d.handle}")

        print("\nSubscribing to notify characteristic ...")
        await client.start_notify(NOTIFY_UUID, on_notify)

        for label, pkt in (("get_state (A3)", get_state()), ("get_info (A8)", get_info())):
            print(f"\n  -> {label}: {hexdump(pkt)}")
            await client.write_gatt_char(WRITE_UUID, pkt, response=False)
            await asyncio.sleep(wait)

        print(f"\nListening {wait}s more for stragglers ...")
        await asyncio.sleep(wait)
        await client.stop_notify(NOTIFY_UUID)
    print("Disconnected.")


if __name__ == "__main__":
    p = argparse.ArgumentParser()
    p.add_argument("address", help="device UUID printed by scan.py")
    p.add_argument("--wait", type=float, default=1.5, help="seconds to wait after each command")
    a = p.parse_args()
    asyncio.run(main(a.address, a.wait))

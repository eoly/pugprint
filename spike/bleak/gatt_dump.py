"""Connect to any device and dump its full GATT table + MTU. No writes.

Usage: python spike/bleak/gatt_dump.py <device-uuid-from-scan.py>
"""

import argparse
import asyncio

from bleak import BleakClient


async def main(address: str) -> None:
    print(f"Connecting to {address} ...")
    async with BleakClient(address, timeout=20.0) as client:
        print(f"Connected. MTU={client.mtu_size}\n")
        for svc in client.services:
            print(f"service {svc.uuid}  ({svc.description})")
            for ch in svc.characteristics:
                print(f"  char {ch.uuid}  handle={ch.handle}  [{','.join(ch.properties)}]  ({ch.description})")
                for d in ch.descriptors:
                    print(f"    desc {d.uuid}  handle={d.handle}")
                if "read" in ch.properties:
                    try:
                        val = await client.read_gatt_char(ch)
                        printable = val.decode("ascii") if all(32 <= b < 127 for b in val) else ""
                        print(f"    value: {val.hex()}  {printable!r}" if printable else f"    value: {val.hex()}")
                    except Exception as e:  # noqa: BLE001
                        print(f"    value: <read failed: {e}>")
    print("\nDisconnected.")


if __name__ == "__main__":
    p = argparse.ArgumentParser()
    p.add_argument("address")
    asyncio.run(main(p.parse_args().address))

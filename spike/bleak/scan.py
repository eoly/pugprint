"""Scan for the printer. Matches on the ae30 service UUID, never on name alone.

Usage: python spike/bleak/scan.py [--seconds N] [--all]
Prints every match's CoreBluetooth UUID (macOS has no MAC), name, RSSI and service list.
"""

import argparse
import asyncio

from bleak import BleakScanner

from catproto import SERVICE_UUID


async def main(seconds: float, show_all: bool) -> None:
    print(f"Scanning {seconds:.0f}s...")
    found = await BleakScanner.discover(timeout=seconds, return_adv=True)
    hits = 0
    for dev, adv in found.values():
        uuids = [u.lower() for u in (adv.service_uuids or [])]
        is_printer = SERVICE_UUID in uuids
        if not (is_printer or show_all):
            continue
        hits += 1
        tag = "PRINTER?" if is_printer else "        "
        print(f"{tag} {dev.address}  rssi={adv.rssi:>4}  name={adv.local_name or dev.name!r}")
        if uuids:
            print(f"         services: {', '.join(uuids)}")
        if adv.manufacturer_data:
            for k, v in adv.manufacturer_data.items():
                print(f"         mfr 0x{k:04x}: {v.hex()}")
    if hits == 0:
        print("No ae30 devices found. Is the printer awake? Try --all to see everything.")


if __name__ == "__main__":
    p = argparse.ArgumentParser()
    p.add_argument("--seconds", type=float, default=8.0)
    p.add_argument("--all", action="store_true", help="list every device, not just ae30")
    a = p.parse_args()
    asyncio.run(main(a.seconds, a.all))

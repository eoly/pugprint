# Phase 0 spike workspace — DONE 2026-09-20

Throwaway tooling for the protocol discovery spike. Plan, outcome and exit criteria:
`docs/PHASE0_SPIKE.md`. Confirmed protocol: `docs/PRINTER_PROTOCOL.md`. Nothing here is
built by Gradle or CI.

## Result in one line
Hello Blink = ESC/POS-style `GS v 0` raster printer behind a Microchip transparent-UART
GATT service; needs a density command first; over BLE each raster block must fit in one
write. **Not** a cat printer.

## Setup
```
brew install openjdk@17 python@3.12 jadx
brew install --cask android-platform-tools nrf-connect wireshark-app   # wireshark prompts for sudo
python3.12 -m venv spike/.venv && source spike/.venv/bin/activate
pip install -r spike/requirements.txt
```

## Scripts that matter (`spike/bleak/`)
```
python spike/bleak/scan.py --all                        # printer shows as HB-xxxx, service 49535343-fe7d-…
python spike/bleak/gatt_dump.py <uuid>                  # GATT table + MTU, no writes
python spike/bleak/uart_probe.py <uuid> "1e 47 03"      # raw hex over UART, dump replies
python spike/bleak/vendor_print.py <uuid> black         # the official app's exact sequence
python spike/bleak/vendor_print.py <uuid> photo.png --density 25 --block-rows 1
python spike/bleak/vendor_print.py --dump widths        # regenerate fixtures, no BLE
```
`vendor_print.py` knobs: `--density` (this unit: 20/25/30), `--block-rows` (1 confirmed;
4 = one BLE write; 8 fails), `--delay` between BLE writes, `--block-pause` between blocks.
Shared helpers: `escpos.py` (UUIDs, `pack_row`), `escpos_print.py` (test patterns).

Superseded, kept as a record of refuted hypotheses — do not extend:
`catproto.py`, `probe.py`, `print_test.py`, `dump_commands.py` (cat-printer framing);
`escpos_variants.py`, the `--encoding column/column8/raster` paths in `escpos_print.py`
(encodings without the density command).

## Findings
- `01-gatt-scan.md` — scan, GATT table, every print attempt and what it did on paper.
- `02-apk-notes.md` — jadx read of the official app: transport, print sequence, commands,
  this unit's status replies.
- `03-capture-notes.md` — HCI snoop (not needed; left as a template).

## Fixtures for Phase 2 (`spike/fixtures/`)
- `print_job/*.vendor.hex` + `.pbm` — byte-exact jobs: `black_48_rows`, `stripe_48_rows`,
  `widths_48_rows`, `pug_arrow` (printed correctly on hardware). Format: `# label` line
  then hex, one part per line; raster is 1 row per `GS v 0` block, density 25.
- `refuted/` — fixtures from wrong hypotheses. Ignore.

## Not committed
`apk/` (vendor XAPK + jadx output — proprietary, gitignored), `.venv/`.

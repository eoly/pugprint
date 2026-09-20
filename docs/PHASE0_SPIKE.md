# Phase 0 — Protocol Discovery Spike

**Goal:** replace every INFERRED claim in `PRINTER_PROTOCOL.md` with a fact confirmed
against a real Hello Blink unit, and check in golden fixtures so Phase 2 can build
`:core:printer` without hardware.

**Branch:** `feat/phase0-protocol-spike`. **Working dir:** `spike/` at the repo root
(Gradle never sees it, so CI is unaffected once Phase 1 lands).

## Outcome (2026-09-20) — DONE
The cat-printer hypothesis was **refuted in step 2**: no `ae30` service. The printer is an
ESC/POS-style raster printer over a Microchip transparent-UART GATT service. Step 4 (bleak)
got status replies but no prints; step 3 (jadx of the APKPure XAPK, since no phone with the
app was available) revealed the missing density command and that the app uses RFCOMM;
replaying its sequence over BLE with small raster blocks printed a test image. Step 5 (HCI
snoop) was unnecessary. Findings: `spike/01-gatt-scan.md`, `spike/02-apk-notes.md`;
consolidated in `docs/PRINTER_PROTOCOL.md` and `docs/adr/0005-ble-uart-not-rfcomm.md`.

## Exit criteria (Definition of Done)

- [x] Advertised name recorded (`HB-nnnn`, SN suffix); ae30 service **refuted** — `49535343-fe7d-…` instead.
- [x] Service/characteristic UUIDs + properties + negotiated MTU (248) recorded.
- [x] Exact bytes for every command (no framing, no CRC — plain ESC/POS-style commands).
- [x] Density defaults the official app sends (20/25/30 for this unit; 25 = medium).
- [x] Row format: `GS v 0`, 48 B/row, MSB-first, 1 = black; feed = `0a 0a 0a 0a`.
- [x] Pacing: one `GS v 0` block per BLE write (1 row confirmed, 4 rows see notes), ~20 ms apart.
- [x] Official APK: targetSdk 34, minSdk 26, arm64-v8a only, permissions listed.
- [x] Successful print of a known bitmap ("PUG →", 384×96) from `vendor_print.py`.
- [x] Golden fixtures under `spike/fixtures/print_job/*.vendor.hex`.
- [x] `PRINTER_PROTOCOL.md` rewritten; ADR 0005 added.
- [ ] Not done (deferred to Phase 3 hardware checklist): error strings (`err:`) observed
      live for cover-open / out-of-paper; max safe pacing rate; battery-low threshold.

## Prerequisites

- The Hello Blink printer, charged, with paper.
- A phone that still has the official app installed (Android strongly preferred — needed
  for the APK pull and the HCI snoop log).
- This Mac.

## 1. Tooling

Homebrew Python is 3.14; the dev-environment doc pins 3.12. Use a 3.12 venv to avoid
`bleak` wheel surprises. `jadx` and Wireshark are referenced by the protocol doc but are
not in the Brewfile — install them here.

```
brew install openjdk@17 python@3.12 jadx
brew install --cask android-platform-tools nrf-connect wireshark-app   # wireshark prompts for sudo
python3.12 -m venv spike/.venv && source spike/.venv/bin/activate
pip install -r spike/requirements.txt
```

`spike/.venv/` and `spike/apk/` are gitignored. Scripts and per-step note templates are
already scaffolded under `spike/` — see `spike/README.md`.

Layout:
```
spike/
  01-gatt-scan.md          # step 2 output
  02-apk-notes.md          # step 3 output
  03-capture-notes.md      # step 5 output
  bleak/
    scan.py                # find printer by ae30 service, print CoreBluetooth UUID
    probe.py               # connect, notify on ae02, send A3/A8, hex-dump replies
    print_test.py          # frame encoder + test-bitmap print
  captures/*.pcapng        # HCI snoop logs (step 5)
  fixtures/                # step 6 — golden bytes for Phase 2
```

## 2. nRF Connect scan (do this first)

This is the ten-minute test that validates or kills the whole cat-printer assumption.

1. Power on the printer; scan in nRF Connect (desktop or phone).
2. Record: advertised name, address, raw advertising data, full GATT table with
   properties (write / write-no-response / notify), and MTU if shown.
3. Save to `spike/01-gatt-scan.md`.

**Decision gate:** if the service is **not** `0000ae30-…`, stop. Hello Blink may be a
Phomemo / Paperang / Peripage-style printer with a different protocol; re-plan before
spending time on steps 3–5.

## 3. Pull and decompile the official APK

Pull from the phone, never from a mirror site.

```
adb shell pm path com.efercro.helloblink      # package name is itself an assumption — verify
adb pull <path> spike/apk/helloblink.apk
jadx-gui spike/apk/helloblink.apk
```

Search the decompiled source for `ae01`, `5178`, `0x51`, `energy`, `crc`, `0xA2`, `0xBF`.
Record in `spike/02-apk-notes.md`:
- Command constants and the CRC implementation (table vs polynomial; which commands use
  hardcoded CRCs).
- Energy/quality defaults per mode (image vs text).
- Row encoding path: raw, RLE, or LZO; any per-model branches and how the model is detected.
- Any `0x12` frame prefix or `ae03`/`ae04` usage.
- Manifest: targetSdk, permissions. `lib/`: ABIs present (16 KB page relevance).

**Do not commit** the APK or the decompiled tree — notes only. This is interoperability
research: read it, understand it, rewrite from understanding. Same rule as for
NaitLee/Cat-Printer (AGPL): reference only.

## 4. Talk to the printer from `bleak`

Three small scripts in `spike/bleak/`:

- **`scan.py`** — find the printer by ae30 service UUID (never by name alone). Print its
  CoreBluetooth UUID; macOS does not expose the MAC.
- **`probe.py`** — connect, subscribe to ae02, send get-state `0xA3` and get-info `0xA8`,
  hex-dump every notification. Confirms the frame format and CRC before any printing.
- **`print_test.py`** — frame encoder built from the constants confirmed so far; sends the
  setup sequence, rows for a test bitmap, then feed.

Test bitmaps, in order:
1. 8 rows, all black → confirms energy and that rows print at all.
2. Pattern with a known left edge and a 1-px stripe → confirms bit order (LSB-first?) and
   384-dot row width.
3. A dithered photo at 384 px wide → end-to-end sanity.

Pacing: chunk writes to `MTU − 3` and sleep between chunks. Tune the sleep down until rows
corrupt, then back off. **That number is the Phase 3 pacing value — record it.**

Gotchas: the printer sleeps fast; wake it before each run and add a connect retry.
Write-without-response has no ACK, so a "successful" script run proves nothing until the
paper looks right.

## 5. Capture the official app printing (ground truth)

On the Android phone:
1. Developer options → **Enable Bluetooth HCI snoop log**. Toggle Bluetooth off/on.
2. Print **one** simple known image from the official app (ideally the same all-black or
   stripe bitmap as step 4).
3. `adb bugreport spike/captures/bugreport.zip` → extract `btsnoop_hci.log` →
   save as `spike/captures/official-print-1.pcapng`.
4. In Wireshark, filter ATT write commands to the ae01 handle
   (`btatt.opcode == 0x52`), export payloads as hex into `spike/03-capture-notes.md`.

Extract: setup commands, energy value, row command used, feed sequence, and the timing
between writes. Diff against what `print_test.py` sends and fix the script until the
sequences match. Recompute CRC8 over the captured frames to settle the CRC rule.

iPhone-only fallback: Apple's PacketLogger with the Bluetooth logging profile (more work).

## 6. Consolidate into the repo

1. Rewrite `docs/PRINTER_PROTOCOL.md`: confirmed facts, no warning banner, fill the
   command table, add the pacing value and MTU.
2. Add an ADR if the spike forced a decision (e.g., raw `0xA2` rows over `0xBF`).
3. Create golden fixtures in the shape Phase 2 will consume — one file per command plus
   one complete print job of a tiny known bitmap:
   ```
   spike/fixtures/commands/get_state.hex
   spike/fixtures/commands/get_info.hex
   spike/fixtures/commands/set_energy_default.hex
   spike/fixtures/commands/feed_N.hex
   spike/fixtures/print_job/black_8_rows.pbm     # input
   spike/fixtures/print_job/black_8_rows.hex     # exact byte stream, in order
   spike/fixtures/print_job/stripe_8_rows.pbm
   spike/fixtures/print_job/stripe_8_rows.hex
   ```
   Phase 2 moves these into `core/printer/src/test/resources` and asserts the Kotlin
   encoder reproduces them byte for byte.
4. Tick the exit criteria above; open the PR.

## Ordering notes

Steps 3 and 4 can swap. Do 3 first if the phone with the app is at hand (it tells you
exactly what bytes to send). Do 4 first if you want to see the printer respond tonight
using the known-family frames from `rbaron/catprinter`.

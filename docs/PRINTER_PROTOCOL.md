# Hello Blink Printer Protocol — CONFIRMED (Phase 0 spike, 2026-09-20)

**Status:** Confirmed against a real unit (SN `HBHW2500xxxxxxxx`, fw `V1.01`, hw `H1.0`) and
against a jadx read of the official app (`com.efercro.helloblink` 1.1.8). Details and raw
findings: `spike/01-gatt-scan.md`, `spike/02-apk-notes.md`.

**Hello Blink is NOT a cat printer.** It is an ESC/POS-flavoured 58 mm thermal printer
(YHK "rabbit" firmware family) behind a dual-mode Bluetooth module. The old cat-printer
assumptions (`ae30` service, `51 78` frames, CRC8) are all refuted and gone.

## Identity
| Item | Value |
|------|-------|
| Advertised BLE name | `HB-` + last 4 digits of SN, e.g. `HB-1234`. Do not filter on name alone. |
| Advertised service | `49535343-fe7d-4ae5-8fa9-9fafd205e455` (Microchip Transparent UART). **Filter on this.** |
| Manufacturer data | company id `0x0000`, payload `25 00 21 01` + last 4 SN digits as BCD (e.g. `12 34`) |
| Head | **384 dots** (48 bytes/row), 8 dots/mm, 58 mm paper |
| Text engine | none — raster only |

## Transports
The official app uses **Bluetooth Classic SPP/RFCOMM** (UUID `00001101-…`) and streams
everything in one write. PugPrint uses **BLE** (works, see constraints below):

| Role | UUID | Properties |
|------|------|------------|
| Service | `49535343-fe7d-4ae5-8fa9-9fafd205e455` | |
| RX (host → printer) | `49535343-8841-43f4-a8d4-ecbe34729bb3` | write-without-response, write |
| TX (printer → host) | `49535343-1e4d-4bd9-ba61-23c647249616` | notify (enable via 0x2902) |
| control point | `49535343-aca3-481c-91ec-d85e28a60318` | notify, write — unused |
| unknown | `49535343-6daa-4d02-abf6-19569aca69fe` | write — unused |

Negotiated MTU on macOS: 248 → 245-byte writes. No flow control on BLE.

### BLE constraints (measured)
- A `GS v 0` raster block must be **small and paced**. Measured (50 ms between writes):
  | rows/block | bytes | pause after block | result |
  |-----------:|------:|------------------:|--------|
  | 1 | 56 | 20 ms | **prints completely** (widths, PUG, dither image) |
  | 4 | 200 | 20 ms | ~¼ of rows printed (buffer overrun) |
  | 8 | 392 | 20 ms | nothing (block spans two writes) |
  | 2 | 104 | 40 ms | ~½ of rows printed |
  | 4 | 200 | 100 ms | ~½ of rows printed |
  Multi-row blocks lose rows regardless of pause length — it is not simply pacing.
  **Rule for Phase 3: 1 row per `GS v 0` block, one block per BLE write, ~20 ms apart.**
  Never split a block across writes. If more speed is needed, try RFCOMM (ADR 0005 fallback).
- A single large block (whole image, as the app sends over RFCOMM) prints only its first
  ~5 rows over BLE, then the rest is discarded.
- Pace ~20 ms between blocks; 50 ms between writes was used throughout the spike without
  corruption. Faster is untested.
- `LABELOK` arrives on TX at unpredictable times (label-sensor event). **Not an ack**; ignore.

## Print sequence (byte-exact, from `TaskPosPrint.run` in the official app)
```
1d 49 f0 <density>      GS I 0xF0 n   heat. This unit ("public" factory type): 20 light / 25 medium / 30 dark
1b 40                   ESC @         init
1d 76 30 00 30 00 yL yH + rows   GS v 0, m=0, xL=0x30 (48 bytes = 384 dots), y rows,
                                 1 bpp, MSB-first, bit=1 → black, pixel 0 = left  — repeat per block
(250 ms pause)
0a 0a 0a 0a             feed (4 × LF)
```
Confirmed on paper: 1-dot lines resolve at density 25; both paper edges reachable; a
dithered 384×96 text/arrow image printed with correct orientation.

Density values in the app depend on firmware/factory type (`AppConst.setPrintValue`):
| Type | light | medium | dark | extra |
|------|-------|--------|------|-------|
| public (product id ≥ 200) — **this unit** | 20 | 25 | 30 | — |
| private, fw ≥ 119 ("new") | 12 | 15 | 18 | — |
| private, fw < 119 ("old") | 5 | 10 | 15 | also sends speed `1d 49 f1 1e` |
Default in the app is medium. Density 15 printed on this unit but is lighter; 25 is the
right default. Optional: `1d 49 f8 <n>` copies (only if n > 1).

## Query / status commands
| Bytes | Name | Reply (ASCII, NUL-terminated) |
|-------|------|-------------------------------|
| `1e 47 03` | status | `HV=H1.0,SV=V1.01,VOLT=7540mv,DPI=384,` — hw/fw version, battery mV, head width |
| `1d 67 39` | serial | `sn:HBHW2500xxxxxxxx.` (16 chars; last 4 = advertised-name suffix) |
| `1d 67 69` | product | `public id:0801.` (`id` < 200 → "private") |
| `10 04 01..04` | ESC/POS DLE EOT | 1 status byte each (`1e`, `1a`, `12`, `12` when idle & paper OK) |
| `1d 72 01` | GS r 1 paper | `00` = paper present |
| `1d 61 ff` | GS a — ASB | `00 00 00 00` |
| async | error | `err:\x02.` cover open · `err:\x00.` cleared · `err:\x04` / `err:\x10` other (see app strings) |
| `LABELV1` / `LABELAT1` | label calibrate / next | `LABELOK` |
Not implemented: `GS I n` (printer ID), `ESC v`, TSPL `~!T`, text printing.

Do **not** send `1b 23 23 55 50 50 47` (`##UPPG` — firmware update mode).

## Image pipeline the app uses (for parity in `:core:imaging`)
Resize to 384 wide → `GPUImageSharpenFilter(0.5)` → custom error-diffusion dither
(`ImageUtil.mypxByte2`: luminance `(38R+75G+15B)>>7`, Floyd–Steinberg weights 7/3/5/1 applied
column-major) → pack MSB-first. Plain Floyd–Steinberg (Pillow `convert("1")`) prints fine.

## Golden fixtures
`spike/fixtures/print_job/*.vendor.hex` — byte-exact jobs (density · init · raster blocks ·
feed) for `black`, `stripe`, `widths`, and `pug_arrow`, plus matching `.pbm` inputs. Phase 2
moves these into `core/printer/src/test/resources`.

## Spike tooling
`spike/bleak/vendor_print.py` prints any PNG via the sequence above
(`--density`, `--block-rows`, `--delay`); `uart_probe.py` sends raw hex and dumps replies;
`gatt_dump.py` lists the GATT table. See `spike/README.md`.

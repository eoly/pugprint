# Step 2 — nRF Connect / bleak scan findings

Date: 2026-09-20
Printer firmware / sticker info:

## Advertising (bleak `scan.py --all`, 10 s, printer next to the Mac)
- Advertised name: `HB-nnnn` (nnnn = last 4 digits of the serial; scrubbed)
- Address (macOS CoreBluetooth UUID): per-Mac random UUID (scrubbed; get yours from `scan.py`)
- Advertised service UUIDs: `49535343-fe7d-4ae5-8fa9-9fafd205e455`
  (Microchip/ISSC "Transparent UART" — RN4870/BM7x family; used by Phomemo, Peripage,
  and many generic ESC/POS BLE thermal printers. **NOT the cat-printer `ae30` service.**)
- Manufacturer data: company id `0x0000`, payload `25 00 21 01 nn nn` (trailing `nnnn`
  matches the name suffix; likely a serial/unit number).
- RSSI at ~1 m: -48
- **No `ae30` device seen at all** in the scan.

## GATT table (`gatt_dump.py`, macOS/bleak)
```
MTU=248
service 49535343-fe7d-4ae5-8fa9-9fafd205e455   Microchip Transparent UART
  char 49535343-1e4d-4bd9-ba61-23c647249616  handle=5   [notify]                       TX (printer→host)
    desc 2902                                 handle=7
  char 49535343-6daa-4d02-abf6-19569aca69fe  handle=8   [write]                        (transparent-UART "control"?)
  char 49535343-8841-43f4-a8d4-ecbe34729bb3  handle=10  [write-without-response,write] RX (host→printer)
  char 49535343-aca3-481c-91ec-d85e28a60318  handle=12  [notify,write]                 control point (Microchip TRCP)
    desc 2902                                 handle=14
```
Only one service exposed — no Device Information (180a), no Battery (180f).

## Confirmed / refuted
| Assumption                          | Result |
|-------------------------------------|--------|
| Service `ae30` present              | **REFUTED** — service is `49535343-fe7d-…` (Microchip transparent UART) |
| `ae01` write-no-response            | REFUTED — write is `49535343-8841-…` |
| `ae02` notify                       | REFUTED — notify is `49535343-1e4d-…` |
| `ae03` / `ae04` present?            | n/a |
| Negotiated MTU                      | 248 (→ 245-byte writes) |

## Decision gate
- [ ] ae30 confirmed → continue to steps 3–5.
- [x] ae30 absent → **STOP; identify the real family before continuing.**
  Candidates sharing this GATT: Phomemo (M02/T02 etc.), Peripage, and generic ESC/POS
  58 mm BLE printers. Cat-printer command table in `docs/PRINTER_PROTOCOL.md` is void.

## Family identification (`uart_probe.py`, same day)
The printer answers **ESC/POS** real-time status queries on the UART TX characteristic:

| Sent (hex)  | Command                          | Reply | Meaning (ESC/POS spec) |
|-------------|----------------------------------|-------|------------------------|
| `10 04 01`  | DLE EOT 1 – printer status       | `1e`  | fixed bits 0x12 + bit2 + bit3 (vendor-specific; possibly "online" flags) |
| `10 04 02`  | DLE EOT 2 – offline cause        | `1a`  | fixed 0x12 + bit3 |
| `10 04 03`  | DLE EOT 3 – error status         | `12`  | no errors |
| `10 04 04`  | DLE EOT 4 – paper sensor         | `12`  | paper present |
| `1d 49 01/02/03/41/42/43` | GS I – printer ID  | —     | no reply (not implemented) |
| `1d 72 01`  | GS r 1 – transmit paper status   | `00`  | paper OK |

Unsolicited notification seen after the `text` job (ESC @ + text + ESC d 3):
`4c 41 42 45 4c 4f 4b` = ASCII **`LABELOK`**. Not seen (within 3 s) after raster jobs —
either timing, or it is tied to a label/feed-to-mark event. Vendor status strings like this
sit on top of ESC/POS; watch for others (`NOPAPER`? `LABELERR`?) during error testing.

Second run (`escpos_variants.py`): `LABELOK` came back only after the one variant that
contained a bare **LF (`0a`)** — same as the text job. Raster jobs ending in `ESC d n` alone
never got it. Hypothesis: LF = "commit label / flush print buffer", and `LABELOK` is the ack.

TSPL queries (`~!T`, `~!@`, `~!I`, `ESC !?`) → no reply. Not TSPL.

## Image encoding — CONFIRMED on paper
| Encoding                                   | Prints? | `LABELOK` ack? |
|--------------------------------------------|---------|----------------|
| ESC/POS text + LF                          | no      | yes            |
| `GS v 0` raster (+ `ESC d`)                | no      | no             |
| `GS v 0` + Phomemo `1F 11 xx` header/footer| no      | no             |
| `GS v 0` + `DC2 #` / `GS ( K` density      | no      | no             |
| `GS v 0` + bare LF                         | no      | no             |
| `GS ( L` fn112 + fn50                      | no      | no             |
| **`ESC * 33` column bit-image, 24-dot bands, LF per band** | **YES** | yes (24-row jobs) |

- Text engine: absent (text bytes ignored, LF still acks). **Raster-only via `ESC *`.**
- Orientation: 16-px block at x=0 printed at the **left** edge → column 0 = left, MSB = top dot.
- 1-px-wide vertical lines did **not** print at default energy (stripe test). Width test
  (`widths` pattern: 1,2,3,4,6,8,12 px lines + 8-px block at x=376..383) sent — result TBD.
- 24-row (3 mm) bars read as a "thin line" by eye; use ≥48 rows for visual tests.
- **Pacing / flow control (confirmed):** 2-band job (2326 B) at 245 B per 20 ms → printer
  fed paper, printed NOTHING, no ack. Same bytes at 245 B per 250 ms → prints, `LABELOK`
  arrives right after band 1 while band 2 is still being sent. So the input buffer is
  roughly one band (~1.2 KB) and overflow silently discards the job.
- **`LABELOK` is NOT a flow-control ack.** Ack-gated send (band, wait, band) saw no reply
  for 5 s after band 1. Feed-only probe: `ESC d 3` → nothing, bare LF → nothing, `ESC J 48`
  (8 s later) → `LABELOK`. Looks like a label-gap / feed-distance sensor event (sticker
  firmware), emitted on its own schedule. Do not wait on it.
- **Everything after the first overflow printed BLANK** — 2-band widths at 250 ms/chunk,
  ack-gated 2-band, and a 4-job pacing ladder (96-row black at 150/100/50/20 ms) all fed
  paper with no marks. Only the single-band 24-row jobs *before* the overflow ever printed.
  Status queries (`DLE EOT`, `GS r`) still answer normally in this state.
  Two candidate explanations, to be separated after a power cycle:
  (a) the overflow wedged the image path (parser + feeds still work); or
  (b) `ESC * m=33` is being parsed as an 8-dot mode — the "thin line" for a 24-row black
      bar was really ~1 mm — and multi-band jobs fail for a different reason.
  The "24-dot bar → thin line" and "48-row black is the one that broke" pattern favours (b).
  Test after reset, in order: known-good 24-row stripe (m=33); 8-row stripe via
  `ESC * m=1` (8-dot, 384 B/band); then a slow 2-band job LAST.
- **After power cycle: ALL THREE BLANK**, including the byte-identical 24-row stripe that
  marked paper earlier. Neither (a) nor (b) holds. Status after reset unchanged
  (`1e 1a 12 12`, `GS r 1/2` → `00`, `GS a ff` ASB → `00 00 00 00`, `ESC v` → no reply).
  Remaining explanations: the earlier marks were not real prints (head/tear-bar artefacts),
  or the official app sends a vendor enable/wake/density sequence before image data
  (possibly via the non-standard `49535343-6daa-…` write characteristic). **Stop guessing;
  go to the APK (step 3).**
- Developer note: efercro's other app **WalkPrint** is package
  `com.yhk.rabbit.print.walkprint` — the YHK cat-printer family. That is the origin of the
  wrong assumption in `docs/PRINTER_PROTOCOL.md`; HelloBlink is a separate codebase.

**Conclusion (revised): Hello Blink is an ESC/POS-flavoured 58 mm thermal printer behind a
Microchip transparent-UART BLE bridge.** `GS v 0` raster is NOT implemented; the working
path is `ESC *` column bit-image (exact mode set still being pinned down). Old note follows:
image printing was assumed to be `GS v 0` raster (1 bpp, MSB-first,
48 bytes/row = 384 dots). No custom framing, no CRC.

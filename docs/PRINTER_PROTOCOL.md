# Hello Blink / Cat-Printer Protocol  ⚠️ ASSUMPTIONS — VERIFY IN PHASE 0

**Status:** Hello Blink identity is INFERRED (same publisher as WalkPrint, a cat-printer
app). No public nRF Connect scan of a Hello Blink unit was found. Confirm everything below
against a real device before relying on it.

## Assumed BLE GATT (confirmed for GB0x/MX0x/GT01/YT01 family)
- Service:        `0000ae30-0000-1000-8000-00805f9b34fb`
- Write (no-resp):`0000ae01-...` (control + data)
- Notify:         `0000ae02-...` (status; enable via 0x2902 CCCD)
- Some models also expose `ae03`/`ae04`; some prefix frames with `0x12`.
- Advertised name: UNKNOWN for Hello Blink (family examples: GB01, MX06, GT01, YT01, YHK-XXXX).
  ⚠️ Do NOT filter by name alone; also match on the ae30 service.

## Frame format
`51 78 | CMD | DIR | LEN_LO LEN_HI | PAYLOAD… | CRC8 | FF`
- Magic `0x5178`; DIR `00`=host→printer; LEN is little-endian u16.
- **CRC8 over PAYLOAD only, polynomial 0x07.** (A few print commands use hardcoded CRCs.)
- Image data: **1 bit/pixel, LSB-first, 384 dots/row.**

## Command table (fill/verify during spike)
| Name              | CMD  | Notes / TODO |
|-------------------|------|--------------|
| Get device state  | 0xA3 | returns paper/cover/heat/battery/busy bits on notify |
| Get device info   | 0xA8 | firmware string |
| Set quality/energy| 0xA4 | density; per-model default (e.g. image vs text mode) |
| Set energy        | 0xAF | 0x0000–0xFFFF (if present) |
| Feed paper        | 0xA1 | parameter = feed amount |
| Print row (raw)   | 0xA2 | one packed 1bpp row |
| Print row (RLE)   | 0xBF | run-length compressed row (model-dependent; some use LZO) |
| Drawing/latt mode | 0xBE | image vs text dithering hint |

## Discovery methodology (Phase 0, done on the Mac)
1. **nRF Connect** (desktop/phone): scan → note the printer's advertised name + list GATT
   services/characteristics; confirm ae30/ae01/ae02.
2. **Python + `bleak`** on macOS: connect, enable notify on ae02, send known family frames
   (start with `rbaron/catprinter`), print a test bitmap. macOS uses a UUID, not MAC, as address.
3. **Android HCI snoop log** on a phone that still runs the official app: enable Bluetooth
   HCI snoop logging, print once, pull the log, open in **Wireshark**, and diff the writes.
4. **jadx** decompile of `com.efercro.helloblink` to read the exact command constants,
   energy defaults, and any per-model quirks; also inspect `lib/` for ABIs and read targetSdk.
5. Encode findings as **golden tests** in `:core:printer`.

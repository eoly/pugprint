# Step 3 — Official APK (jadx) findings

Package name: `com.efercro.helloblink` (confirmed)
APK version / versionCode: 1.1.8 / 20 ("Sticker Maker Hello Blink")
Source: APKPure XAPK (user downloaded, 2026-09-20; no device with the app available).
Local: `spike/apk/xapk/` (split APKs), `spike/apk/jadx-out/` (decompiled). Both gitignored.

## Manifest
- targetSdk: **34** (Android 14). minSdk: 26.
- Permissions of note: BLUETOOTH_SCAN/CONNECT/ADVERTISE, legacy BLUETOOTH/ADMIN,
  ACCESS_FINE/COARSE_LOCATION, INTERNET, CAMERA, RECORD_AUDIO, READ_PHONE_STATE,
  READ_MEDIA_IMAGES, FOREGROUND_SERVICE(_DATA_SYNC), RECEIVE_BOOT_COMPLETED, WAKE_LOCK.
  (Contrast: PugPrint needs only BLUETOOTH_SCAN/CONNECT + Photo Picker — a real
  differentiator for the store listing / privacy policy.)
- Split configs: `arm64_v8a` only (no armeabi-v7a / x86 → no 32-bit devices).
- `lib/arm64-v8a/` (12 libs): Bugly crash reporting (Tencent), c++_shared, dict-parser,
  imagepipeline + yuv-decoder + gif (Fresco), jcore121 (JPush), jniPdfium/modpdfium/modft2/
  modpng (PDF viewer), **`libnative-lib.so` (42 KB, custom — inspect)**.
- Assets of note: `b1.bin`/`b2.bin` (1.24 MB each) + `b1.txt`/`b2.txt` (2.48 MB each, hex
  of the .bin?) — likely dither/font tables or embedded firmware; `com.tencent.open.config.json`
  (Tencent QQ SDK). Bugly + JPush + QQ SDK explains the INTERNET/PHONE_STATE permissions.

## Architecture of the official app (jadx, 2026-09-20)
- Built on the **YHK rabbit** codebase (`com.yhk.rabbit.print.*`, same as WalkPrint) plus an
  unused `com.lvrenyang.io` POS SDK and an unused `libnative-lib.so` (CPCL/label builders,
  `PrinterChecker` encryption handshake — `PTR_CheckEncrypt` is stubbed to return 1).
- **Transport: Bluetooth Classic SPP/RFCOMM** (`BluetoothService`, UUID `00001101-…`).
  The app never touches GATT. The printer is dual-mode; the BLE Microchip UART reaches the
  same command parser (status/feeds work over BLE) — whether image data does is being tested.
- RFCOMM has link-level flow control, so the app writes the whole raster in one
  `OutputStream.write()` with no chunking or delays. Over BLE we must pace ourselves.
- Print requires a server-side "granted" flag (`checkprintpermission` → `iprinter.efercro.com`)
  keyed on the printer SN + phone MAC. That, plus targetSdk 34, is likely why it fails on
  new Android. Irrelevant to PugPrint (no network by design).

## Print sequence (`task/TaskPosPrint.run` + `utils/ImageUtil.mypxByte2/printDraw2`)
| Step | Bytes | Notes |
|------|-------|-------|
| density | `1d 49 f0 nn` | `GS I 0xF0 n`. "new" fw (SV ≥ 119): light 12 / **med 15** / dark 18; "old" fw: 5/10/15; "public" factory: 20/25/30. Default NongDu=1 → medium. Custom mode (NongDu=3) divides by 1.6. |
| speed | `1d 49 f1 nn` | only sent to OLD fw (SV < 119); value 0x1e (30). New fw uses 0x32 (60) implicitly. |
| copies | `1d 49 f8 nn` | only if copies > 1 |
| init | `1b 40` | per image |
| raster | `1d 76 30 00 xL xH yL yH` + data | **standard GS v 0**, m=0, xL=48 (384 dots), ONE block for the whole image (height < 99999). MSB-first, 1 = black. Image is resized to DPI width (384 or 576) and sharpened (GPUImage, 0.5) then Floyd–Steinberg-ish dithered (`mypxByte2`, custom 7/3/5/1 kernel on a column-major grid, luminance = (38R+75G+15B)>>7). |
| pause | 250 ms | `Thread.sleep(250)` |
| feed | `0a 0a 0a 0a` | or `LABELAT1` (ASCII) in label mode |

## Vendor query / status commands (`base/AppConst`, `bluetooth/BTCmd`)
| Bytes | Name | Reply parsing |
|-------|------|---------------|
| `1e 47 03` | CMD_STATUS | ASCII containing `SV=xx.yyy,VOLT=n,DPI=384` — fw version (2-char type prefix + number), battery volts, head width |
| `1d 67 39` | CMD_SN | `sn:XXXX.` |
| `1d 67 69` | CMD_PRODUCT | `id:NNN.` (< 200 → "private" factory type, else "public") |
| `1b 23 4e` | battery | |
| `1b 76 01` | state | |
| `1e 56 08` / `1e 56 18` | query / encrypt | |
| `12 41` | text gap | |
| `LABELV1` / `LABELAT1` | label calibrate / next label | printer replies `LABELOK` |
| `1b 23 23 55 50 50 47` | firmware update ("##UPPG") | do not send |
| async `err:\x02.` | cover open | `err:\x00.` = cleared; `err:\x04`, `err:\x10` = other errors (dialog strings txt_error_t5 / t2) |

## This unit's replies over BLE UART (2026-09-20)
| Query | Reply (ASCII, NUL-terminated) | App's interpretation |
|-------|-------------------------------|----------------------|
| `1e 47 03` | `HV=H1.0,SV=V1.01,VOLT=7540mv,DPI=384,` | fw "V1.01" → versionType `v1`, Sversion **101** (< 119 → "old" rule if Private); DPI **384**; battery 7.54 V |
| `1d 67 39` | `sn:HBHW2500xxxxxxxx.` | 16-char SN (scrubbed); last 4 digits = adv name `HB-nnnn` suffix and mfr-data tail |
| `1d 67 69` | `public id:0801.` | id 801 ≥ 200 → **FactoryTYPE = public** → densities light/med/dark = **20 / 25 / 30**, no speed cmd |

So for THIS printer the app would send: `1d 49 f0 19` (25), `1b 40`, `GS v 0` block, 250 ms, `0a0a0a0a`.

## Replaying the vendor sequence over BLE UART
- Exact app sequence, one `GS v 0` block (48 and 96 rows), 245 B chunks / 50 ms, density 15
  and 25 → **"very thin line near the top"** each time, nothing else. Same symptom as the
  first GS v 0 attempts. Reading: the raster command IS recognised, but only the first
  chunk (~237 B of data ≈ 5 rows ≈ 0.6 mm) makes it into the block; the rest is lost
  (inter-byte timeout or receive-buffer limit on the BLE bridge). RFCOMM streams
  continuously so the app never hits this.
- Same image as many small blocks: **8 rows/block (392 B) → nothing; 1 row/block (56 B)
  → prints completely.** So over BLE each `GS v 0` block must be small. Hypothesis: a
  block must arrive within one BLE write (≤ MTU−3 = 245 B, i.e. ≤ 4 rows); 4-row test TBD.
- **CONFIRMED on paper (widths pattern, 1-row blocks, density 25):** all 9 marks printed,
  including the **1-dot-wide line**; 16-px block at left edge and 8-px block at right edge
  both present → **384 dots wide, pixel 0 = left, MSB-first, 1 = black**, exactly as the
  app encodes it. `LABELOK` still appears sporadically; ignore it.
- Working BLE recipe: `1d 49 f0 19` · `1b 40` · N × (`1d 76 30 00 30 00 01 00` + 48 B)
  with ~20 ms between blocks · 250 ms · `0a 0a 0a 0a`. 48 rows ≈ 2.7 KB ≈ 1.5 s.
- **Orientation confirmed** with an asymmetric image ("PUG →", 384×96, 1-row blocks):
  readable, arrow points right. **Dither test** (gradient, grey circle, 4 grey steps, 1-row
  blocks, Pillow Floyd–Steinberg): gradient smooth, all four grey steps distinguishable.
  No need to replicate the app's sharpen filter.
- **Block-size ladder** (50 ms between BLE writes):
  | rows/block | pause | result |
  |---|---|---|
  | 1 | 20 ms | complete |
  | 4 | 20 ms | ~¼ of 96 rows printed → receive-buffer overrun, not a per-write limit |
  | 8 | 20 ms | nothing (spans two writes) |
  | 2 | 40 ms | ~½ of 48 rows printed |
  | 4 | 100 ms | ~½ of 96 rows printed (vs ¼ at 20 ms — pause helps but does not fix it) |
  **Conclusion: over BLE, only 1-row `GS v 0` blocks are reliable.** Multi-row blocks drop
  rows even with generous pauses, so this is a firmware/bridge behaviour, not host pacing.

## Protocol constants (class / file where found)
| Item                   | Value | Source |
|------------------------|-------|--------|
| Magic / header         |       |        |
| CRC impl (table/poly)  |       |        |
| Commands w/ fixed CRC  |       |        |
| Get state              |       |        |
| Get info               |       |        |
| Set quality            |       |        |
| Set energy             |       |        |
| Energy default (image) |       |        |
| Energy default (text)  |       |        |
| Draw mode              |       |        |
| Feed                   |       |        |
| Print row raw          |       |        |
| Print row compressed   |       |        |
| Compression type       |       |        |
| Row width (dots)       |       |        |
| Bit order in byte      |       |        |
| 0x12 prefix?           |       |        |

## Model detection / per-model branches

## Write pacing in the official app (chunk size, delays)

## Notes
Reference only. Do not copy code into the Kotlin modules.

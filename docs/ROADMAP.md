# Roadmap

- **Phase 0 — Protocol discovery spike (Mac): ✅ DONE 2026-09-20.** Outcome: Hello Blink is
  an ESC/POS-style raster printer over a Microchip UART GATT service, not a cat printer.
  Confirmed UUIDs, byte-exact print sequence, density values, BLE block-size limit, and a
  successful image print from `bleak`. See `docs/PRINTER_PROTOCOL.md`, `docs/PHASE0_SPIKE.md`,
  `docs/adr/0005-ble-uart-not-rfcomm.md`.
- **Phase 1 — Skeleton + CI: ✅ DONE 2026-09-20.** Gradle 8.13 wrapper, version catalog,
  `:app` + `:core:printer` + `:core:imaging`, ktlint/detekt/Kover, Roborazzi golden of the
  placeholder home screen, `./gradlew assembleDebug testDebugUnitTest ktlintCheck detekt lint
  verifyRoborazziDebug` green. Compose BOM held at 2026.06.01 and Hilt at 2.58 — newer
  releases require AGP 9 / compileSdk 37 (see CLAUDE.md).
- **Phase 2 — Protocol library: ✅ DONE 2026-09-20.** `:core:printer` — `PrinterCommands` /
  `PrinterQueries`, `RasterBlock` (`GS v 0`, ≤ 4 rows/block, 1 default), `DensityProfile`,
  `PrintJob` with the confirmed BLE pacing, `PrinterReply` / `EscPosStatus` parsers
  (`HV=…,SV=…,VOLT=…,DPI=…`, `sn:`, `id:`, `err:`, `LABELOK`, `DLE EOT`), and
  `CommandDecoder` + `PrinterEmulator` so Phase 3's fake transport and tests need no
  hardware. Golden tests reproduce the Phase 0 fixtures byte-for-byte (now in
  `core/printer/src/test/resources/print_job/`).
- **Phase 2.5 — Platform bump: ✅ DONE 2026-09-20.** AGP 9.4.1 (built-in Kotlin, new DSL),
  Gradle 9.7.1, compileSdk/targetSdk 37, Kotlin 2.4.20, KSP 2.3.12, Compose BOM 2026.09.00,
  Hilt 2.60.1, lifecycle 2.11.0, activity-compose 1.13.0, core-ktx 1.19.0, ktlint-gradle
  14.2.0. Dependabot ignores lifted.
- **Phase 3 — BLE transport: ✅ DONE 2026-09-21 (hardware-verified on a Pixel 10 Pro XL + `HB-0342`).**
  `PrinterTransport` / `FakePrinterTransport` / `PrinterClient` in `:core:printer` (paced
  one-row-per-write sending, paper pre-flight, cover-open abort, progress); new
  `:core:bluetooth` with Kable `BleTransport` (MTU 247, oversize writes refused),
  `CompanionPairing` (CDM filtered on the UART service) and `BluetoothPermissions`
  (`BLUETOOTH_CONNECT` only — ADR 0006); `PrinterManager` (remembered printer,
  auto-reconnect with backoff, battery/cover state, test-page print) and the home screen
  states behind it; `TestPattern` test page in `:core:imaging`. Emulator flow via
  `-Ppugprint.fakePrinter=true`. Hardware run: CDM picker finds the printer, MTU 248
  negotiated, test page prints all five bands with no dropped rows at 20 ms pacing,
  auto-reconnect works. Finding: `err:` code 2 means lid open *or* paper out
  (see `docs/TESTING.md` § Hardware checklist).
- **Phase 4 — Image pipeline + UI: ✅ DONE 2026-09-21, hardware-verified 2026-09-22 (photo and
  drawing printed from the Photo Picker on the Pixel + `HB-0342`).** `:core:imaging` gains `GrayImage` (8-bit luma, rotate / crop / box-filter
  scale), `Dither` (Floyd–Steinberg for photos, threshold for drawings, golden PBMs),
  `CropWindow` (the pure pan/zoom/shape maths behind the crop frame) and `ImagePipeline`
  (crop → 384 px → dither, capped at 1152 rows). App: system Photo Picker (no storage
  permission), `ContentResolverPhotoSource` (downsized decode, EXIF orientation), a two-step
  editor — "Make it fit" (pinch/drag, Square / Tall / Wide / Whole, rotate) then "Ready to
  print?" (the actual dots, Photo / Drawing style, Print) — `PrinterManager.printImage`,
  navigation-compose, ViewModel tests and Roborazzi goldens for seven editor states.
- **Phase 5 — Polish + kid features: IN PROGRESS (started 2026-09-22).** Built so a later
  "designer" phase can change themes, layout and add fun content without touching the printing
  core (ADR 0007). PR-sized tasks, each independently testable:
  1. ✅ **Design kit** — `:ui:design`: theme tokens, `ThemeCatalog` (Pug / Bubblegum / Ocean),
     `KidScreen` / `BigButton` / `ChoiceRow` / `StatusBanner`, WCAG contrast tests over the
     catalog, one gallery golden per theme; home + editor rebuilt on the kit;
     `docs/DESIGN_KIT.md`.
  2. **Settings + theme picker + darkness** — `SettingsStore` (theme id, `DensityLevel`), "Pick
     a look" on the home screen, Light / Medium / Dark choice on the preview step,
     `PrinterManager.printImage(bitmap, density)`; `PrintJob` golden per level.
  3. **Friendly error UX + print again** — every `PrintFailure` / offline reason mapped to a
     kid-readable line plus a "what to do" hint in a `StatusBanner` (no more snackbars),
     "Print again" for the last sticker, printing progress with a percentage.
  4. **Sticker document + text captions** — `:core:imaging` `Sticker` / `Layer` model and a
     pure-JVM `StickerRenderer` (golden PBMs); `TextLayer` via a `PixelFont` (bitmap glyphs,
     integer-scaled) so words print crisp; "Add words" step in the editor.
  5. **Stamps** — `StampCatalog` (PBM assets + one line each), stamp layer, big stamp picker.
  6. **Drawing canvas** — `Stroke` model + pure `StrokeRasterizer` (goldens), "Draw a sticker"
     home entry with fat brushes, eraser and undo.
  7. **Accessibility pass** — content descriptions, TalkBack order, 1.5× font-scale goldens,
     lint accessibility checks on.
  8. **Sticker rolls** — `StickerRoll` catalog in `:core:printer` (one entry per roll; the
     standard roll measured 2026-09-22: 49.2 × 49.2 mm square labels, 12.7 mm gap with a
     serration halfway, so pitch ≈ 495 rows at 8 dots/mm), a roll setting, and per-roll
     sizing: crop shape locked to the label, rows capped to its height, feed to the
     serration (`LABELAT1` / `LABELOK` if the firmware honours it, else a computed line feed).
     Golden `PrintJob` per roll. Hardware check: where the print lands on the label.
  9. **Designer handbook + gallery screen** — grow `docs/DESIGN_KIT.md` with stamps/fonts;
     a hidden "Design gallery" screen in debug builds so the designer sees her work live.
- **Phase 6 — Play:** internal track to the friend group → (if going public) closed test
  (12 testers/14 days) → production.

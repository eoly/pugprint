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
- **Phase 3 — BLE transport:** Kable `BleTransport` on the `49535343-…` UART service, CDM
  pairing, one raster block per write with pacing, real-device printing.
- **Phase 4 — Image pipeline + UI:** `:core:imaging` dithering, Photo Picker, crop/rotate,
  preview, print flow.
- **Phase 5 — Polish + kid features:** text/stamps/drawing, density slider, error UX,
  accessibility, large touch targets.
- **Phase 6 — Play:** internal track to the friend group → (if going public) closed test
  (12 testers/14 days) → production.

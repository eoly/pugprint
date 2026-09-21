# Changelog

All notable changes to PugPrint are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and versions follow SemVer.

## [Unreleased]

### Added
- Phase 2 protocol library in `:core:printer`: `PrinterCommands` / `PrinterQueries`
  (density, speed, copies, init, feed, status/serial/product/paper queries), `RasterBlock`
  (`GS v 0`, 1–4 rows per block), `DensityProfile` (public / private-new / private-old
  tables), `PrintJob` (vendor sequence with BLE pacing), `PrinterReply` + `EscPosStatus`
  reply parsers, and `CommandDecoder` + `PrinterEmulator` for hardware-free printing.
  Golden tests reproduce the four Phase 0 fixtures byte-for-byte and round-trip them.
- Phase 1 skeleton: Gradle 8.13 wrapper, version catalog, `:app` (Compose + Hilt),
  pure-JVM `:core:printer` and `:core:imaging`, ktlint/detekt/Kover, Roborazzi golden
  screenshot of the placeholder home screen, GitHub Actions CI.
- `MonoBitmap` 1bpp packing (MSB-first, 1 = black) with tests.
- Printer GATT UUIDs and head constants (384 dots, 48 bytes/row) with tests.

### Changed
- Platform bump (Phase 2.5): Gradle 8.13 → 9.7.1, AGP 8.13 → 9.4.1 with built-in Kotlin
  (`:app` no longer applies `org.jetbrains.kotlin.android`), Kotlin 2.2.20 → 2.4.20,
  KSP → 2.3.12, compileSdk/targetSdk 36 → 37, Compose BOM → 2026.09.00, Hilt → 2.60.1,
  androidx.hilt → 1.4.0, lifecycle → 2.11.0, activity-compose → 1.13.0, navigation → 2.10.1,
  core-ktx → 1.19.0, ktlint-gradle → 14.2.0. Dependabot ignores for these packages removed.
- Spike print-job fixtures moved from `spike/fixtures/print_job/` to
  `core/printer/src/test/resources/print_job/`.
- Phase 0 protocol discovery documented in `docs/PRINTER_PROTOCOL.md`.
- JUnit 5.14 → JUnit 6.1 (platform, Jupiter, vintage moved to a single version).
- Dependabot ignores the packages that need AGP 9 / compileSdk 37 until the platform bump.

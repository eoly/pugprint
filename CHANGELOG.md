# Changelog

All notable changes to PugPrint are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and versions follow SemVer.

## [Unreleased]

### Added
- Phase 5 friendly errors. Every message the home screen shows (printed, out of paper, lid
  open, printer lost, pairing cancelled, permission denied…) is now a `StatusBanner` in a
  kid's words with a second line saying what to do; it clears itself after eight seconds. The
  printer status banner gains the same hints for offline reasons and shows "40% done" while
  printing. "Print it again" repeats the last sticker at the same darkness
  (`PrinterManager.lastPrint` / `printAgain`). The snackbar is gone.
- Phase 5 settings. `SettingsStore` / `AppSettings` (theme id and print density, in app-private
  preferences, nothing identifying). "Pick a look" on the home screen: a `ThemePicker` kit
  component that draws every `ThemeCatalog` theme in its own colours; the chosen theme is
  applied app-wide and remembered. "How dark?" (Lighter / Normal / Darker) on the preview
  step drives `PrinterManager.printImage(bitmap, density)` and the test page, mapped through
  the printer's own `DensityProfile`. `PugTheme.emoji` removed (a colour swatch identifies a
  theme in the picker).
- Phase 5 design kit (ADR 0007). New `:ui:design` module: `PugTheme` / `PugPalette` tokens,
  `ThemeCatalog` (Pug, Bubblegum, Ocean — add a theme by adding one entry), `PugSpacing` /
  `PugTouch` (64 / 56 / 48 dp targets) / `PugLayout` (480 dp content cap for tablets), and the
  components every screen is built from: `KidScreen`, `HeroTitle`, `BigButton` (Primary /
  Secondary / Quiet), `ChoiceRow` (big radio tiles replacing chips) and `StatusBanner`
  (Info / Working / Problem / Success with optional hint and progress). `ThemeCatalogTest`
  checks WCAG AA contrast for every text/background pair of every theme; a Roborazzi gallery
  golden is recorded per theme. Home and editor screens rebuilt on the kit: printer status,
  battery and lid/paper problems are banners, shape and style pickers are big tiles, every
  action is at least 48 dp tall. `docs/DESIGN_KIT.md` is the designer's handbook.
- Phase 4 image pipeline and editor. `:core:imaging` gains `GrayImage` (8-bit luminance with
  quarter-turn rotation, crop and box-filter scaling), `Dither` (Floyd–Steinberg `PHOTO`,
  threshold `DRAWING`) with golden PBMs, `CropWindow` (pan/zoom/shape crop maths in frame
  widths) and `ImagePipeline` (crop → 384 px → ≤ 1152 rows → dither). App: "Print a photo"
  opens the system Photo Picker (no storage permission), `ContentResolverPhotoSource` decodes
  the picture to at most 1600 px with EXIF orientation, and a two-step editor lets a kid fit
  the picture into a Square / Tall / Wide / Whole sticker by pinching and dragging, rotate it,
  see the exact dots that will print, switch Photo / Drawing style and print.
  `PrinterManager.printImage(MonoBitmap)`; navigation-compose for home ↔ editor; ViewModel
  tests and seven Roborazzi goldens for the editor.
- Phase 3 BLE transport. `:core:printer` gains the transport abstraction (`PrinterTransport`,
  `TransportState`, `PrinterDevice`), `FakePrinterTransport` over the emulator, and
  `PrinterClient` (identify → `PrinterIdentity` with density profile and battery; paced
  one-row-per-write printing with progress, `GS r 1` paper pre-flight, `err:` lid/paper
  abort, `PrintResult`). New Android module `:core:bluetooth`: Kable `BleTransport` on the
  Hello Blink UART service (MTU 247 requested, oversize writes refused so a block is never
  fragmented, TX observed for the whole connection), `CompanionPairing` (Companion Device
  Manager filtered on the service UUID) and `BluetoothPermissions`. `:core:imaging` gains
  `TestPattern`, a 384-dot test page with a golden PBM.
- App: `PrinterManager` (remembers the paired printer, connects on launch, identifies,
  reconnects with exponential backoff, tracks lid/paper errors, prints the test page), Hilt wiring,
  home screen with connect / print-test-page / retry / forget, battery and progress display,
  snackbar feedback; Roborazzi goldens for six states. `-Ppugprint.fakePrinter=true` builds
  against the in-process emulator for Android emulators without Bluetooth.
- Manifest: `BLUETOOTH_CONNECT` (Android 12+) and legacy `BLUETOOTH` (≤ 30); `bluetooth_le`
  required, `companion_device_setup` optional. No scan or location permission (ADR 0006).
- `PrinterEmulator.paperPresent` to emulate an empty paper bay.

### Fixed
- Hardware run 2026-09-21: the Hello Blink sends `err:` code 2 for an empty paper bay as well as
  an open lid, so the app now says "Close the lid and check the paper" (`ErrorKind.LID_OR_PAPER`,
  `PrintFailure.LID_OR_PAPER`) instead of claiming the lid is open.
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

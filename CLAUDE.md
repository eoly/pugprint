# PugPrint

Native Android app (Kotlin + Compose) that prints images to a "Hello Blink" 58 mm
thermal sticker printer over BLE. It speaks ESC/POS-style `GS v 0` raster over a Microchip
transparent-UART GATT service (NOT a cat printer — see docs/PRINTER_PROTOCOL.md).
Offline; no network, accounts, analytics, ads.

## Stack
- Kotlin 2.4 (K2), AGP 9.4 with built-in Kotlin (`:app` applies no `kotlin-android` plugin;
  `:core:*` apply `kotlin-jvm`), Gradle 9.7, JDK 17, KSP 2.3 (versioned independently of Kotlin)
- compileSdk/targetSdk 37 (Android 17), minSdk 26
- Jetpack Compose (BOM 2026.09), Hilt 2.60, Kable for BLE, Coroutines/Flow
- Pinned versions live in `gradle/libs.versions.toml`; it is the source of truth over this list.
  Dependabot proposes bumps weekly; CI must stay green on them.

## Commands
- Build:        ./gradlew assembleDebug
- Unit tests:   ./gradlew testDebugUnitTest
- Lint/format:  ./gradlew ktlintCheck detekt lint
- Instrumented: ./gradlew connectedDebugAndroidTest
- Screenshots:  ./gradlew verifyRoborazziDebug   (record: recordRoborazziDebug)
- Install:      adb install -r app/build/outputs/apk/debug/app-debug.apk

## Architecture rules
- `:core:printer` is PURE Kotlin/JVM — protocol encode/decode, the `PrinterTransport`
  abstraction, `FakePrinterTransport`, `PrinterClient` + golden tests. NO Android imports.
- `:core:imaging` is PURE Kotlin/JVM — dithering + scale-to-384px + `TestPattern` + golden tests.
- `:core:bluetooth` is the ONLY module that imports Android Bluetooth APIs (Kable
  `BleTransport`, Companion Device Manager pairing, permission helpers). No protocol bytes.
- All printer I/O goes through the `PrinterTransport` interface. `FakePrinterTransport`
  wraps `PrinterEmulator` so dev/tests run without hardware
  (`./gradlew installDebug -Ppugprint.fakePrinter=true` for emulators).
- Pairing is CDM-only; permissions are `BLUETOOTH_CONNECT` (+ legacy `BLUETOOTH` ≤ 30).
  Never add `BLUETOOTH_SCAN` or location without revising ADR 0006.
- UI = MVVM, unidirectional data flow, `StateFlow`. Business logic never in Composables.
- kotlinx-coroutines-test: `advanceUntilIdle()` skips background-only coroutines; use
  `runCurrent()` / `advanceTimeBy()` when the code under test launches into `backgroundScope`.

## Do
- Add tests with every change (golden tests for any protocol/imaging code).
- Keep protocol bytes ONLY in `:core:printer`.

## Don't
- Don't add analytics, ads, networking, accounts, or location code — this is a kids' app.
- Don't put Android framework imports in `:core:*` modules.
- Don't copy AGPL code (e.g., NaitLee/Cat-Printer) or decompiled vendor code into this repo;
  reference only. `spike/apk/` is gitignored and must stay that way.
- Don't hardcode a printer MAC or advertised name — discover at runtime by service UUID
  `49535343-fe7d-4ae5-8fa9-9fafd205e455`.
- Don't send multi-row `GS v 0` blocks over BLE — the printer drops rows. 1 row per block,
  one block per write, paced (see docs/PRINTER_PROTOCOL.md).

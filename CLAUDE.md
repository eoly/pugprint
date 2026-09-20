# PugPrint

Native Android app (Kotlin + Compose) that prints images to a "Hello Blink"
cat-family thermal printer over BLE. Offline; no network, accounts, analytics, ads.

## Stack
- Kotlin 2.2.20 (K2), AGP 8.13, Gradle 8.13, JDK 17
- compileSdk/targetSdk 36 (Android 16), minSdk 26
- Jetpack Compose (BOM 2026.09.00), Hilt DI, Kable for BLE, Coroutines/Flow

## Commands
- Build:        ./gradlew assembleDebug
- Unit tests:   ./gradlew testDebugUnitTest
- Lint/format:  ./gradlew ktlintCheck detekt lint
- Instrumented: ./gradlew connectedDebugAndroidTest
- Screenshots:  ./gradlew verifyRoborazziDebug   (record: recordRoborazziDebug)
- Install:      adb install -r app/build/outputs/apk/debug/app-debug.apk

## Architecture rules
- `:core:printer` is PURE Kotlin/JVM — protocol encode/decode + golden tests. NO Android imports.
- `:core:imaging` is PURE Kotlin/JVM — dithering + scale-to-384px + golden tests.
- All printer I/O goes through the `PrinterTransport` interface. `FakePrinterTransport`
  renders received rows to a bitmap so dev/tests run without hardware.
- UI = MVVM, unidirectional data flow, `StateFlow`. Business logic never in Composables.

## Do
- Add tests with every change (golden tests for any protocol/imaging code).
- Keep protocol bytes ONLY in `:core:printer`.

## Don't
- Don't add analytics, ads, networking, accounts, or location code — this is a kids' app.
- Don't put Android framework imports in `:core:*` modules.
- Don't copy AGPL code (e.g., NaitLee/Cat-Printer) into this repo; use it only as reference.
- Don't hardcode a printer MAC or advertised name — discover at runtime.

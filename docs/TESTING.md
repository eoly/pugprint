# Testing Strategy

## What runs where
| Layer | Tool | Runs on |
|-------|------|---------|
| Protocol encode/decode | JUnit5 + **golden files** | JVM (fast) |
| Dithering/scaling | JUnit5 + golden PNGs | JVM |
| ViewModels/flows | JUnit5 + Turbine + kotlinx-coroutines-test + MockK | JVM |
| Compose UI screenshots | **Roborazzi** (Robolectric) | JVM |
| Compose interaction | Compose UI test | Emulator / Gradle Managed Device |
| End-to-end print | Manual hardware checklist | Real printer |

## Commands
```
./gradlew testDebugUnitTest
./gradlew recordRoborazziDebug      # regenerate golden screenshots
./gradlew verifyRoborazziDebug      # fail on visual diff
./gradlew connectedDebugAndroidTest # instrumented (emulator)
```

`:app` unit tests run on the JUnit Platform: JUnit 5 for ViewModels, and the JUnit 4
Robolectric/Roborazzi tests via `junit-vintage-engine`. `:core:*` are JUnit 5 only; each
exposes a `testDebugUnitTest` alias so the single command above covers every module.
Roborazzi goldens live in `app/screenshots/` (committed).

## Golden tests (highest ROI)
- `:core:printer`: `PrintJobGoldenTest` asserts `PrintJob.writes()` reproduces the vendor
  jobs in `core/printer/src/test/resources/print_job/*.vendor.hex` byte-for-byte, and
  `PrinterEmulatorTest` replays them back into the matching `.pbm` rows — this validates
  the protocol WITHOUT hardware. Regenerate fixtures with
  `python spike/bleak/vendor_print.py --dump <pattern>`.
- `:core:imaging`: `TestPatternTest` asserts the built-in test page against
  `core/imaging/src/test/resources/test_pattern.pbm`; re-record with
  `./gradlew :core:imaging:test -Dpugprint.recordGoldens=true`. Dithering goldens follow in Phase 4.

## Transport tests (no hardware)
- `:core:printer` `PrinterClientTest`: drives `PrinterClient` over `FakePrinterTransport` under
  `runTest` virtual time — asserts every row reaches the emulator with no BLE-rule violation,
  the pacing adds up (`(rows − 1) × 20 ms + 250 ms`), and link loss / paper out / `err:`
  cover-open come back as the right `PrintResult.Failure`.
- `:core:bluetooth` `BleTransportTest`: MockK `Peripheral` — RX write-without-response,
  oversize payload refused, TX relayed, LOST vs requested disconnect, failed connect.
  `CompanionPairingTest` (Robolectric): CDM result decoding. Note: kotlinx-coroutines-test's
  `advanceUntilIdle()` ignores background-only work; use `runCurrent()` / `advanceTimeBy()`.
- `:app` `PrinterManagerTest` / `HomeViewModelTest`: pairing → connect → identify → print →
  reconnect, all on the fake transport.

## Emulator (no Bluetooth)
`./gradlew installDebug -Ppugprint.fakePrinter=true` wires `FakePrinterTransport` and instant
pairing so the whole flow can be clicked through; the "printed" rows live in the emulator object.

## Hardware checklist (run on the real printer before each release)
- [ ] Pair via CDM on a fresh install ("Nearby devices" prompt on Android 12+, then the system picker lists `HB-nnnn`).
- [ ] Home screen shows "HB-nnnn is ready" with a battery percentage.
- [ ] Print the test page: black band edge to edge, 1-dot bars resolved, no missing rows
      (if rows drop, lower `PrintTiming.BLOCK_GAP_MILLIS` is NOT the fix — see PRINTER_PROTOCOL.md).
- [ ] Print full-black test row (verifies energy/density).
- [ ] Print a photo (dithered) and a line drawing (threshold).
- [ ] Out-of-paper and cover-open surface correct errors.
- [ ] Low-battery warning surfaces.
- [ ] Disconnect (walk away) then auto-reconnect.
- [ ] Print immediately after device wake from sleep.

## Static analysis
`./gradlew ktlintCheck detekt lint` + Kover coverage. Spotless ties formatting together.

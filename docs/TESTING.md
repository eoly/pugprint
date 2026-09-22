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
Last run: **2026-09-21**, Pixel 10 Pro XL (Android 17) + Hello Blink `HB-0342` — all items below passed
unless marked.
- [x] Pair via CDM on a fresh install ("Nearby devices" prompt on Android 12+, then the system picker lists `HB-nnnn`).
      Picker found the printer within ~1 s of the printer advertising.
- [x] Home screen shows "HB-nnnn is ready" with a battery percentage (67 % ≈ 7.6 V).
- [x] Print the test page: black band edge to edge, 1-dot bars resolved, all five bands, no missing rows.
      MTU negotiated 248 (247 requested). If rows drop, lowering `PrintTiming.BLOCK_GAP_MILLIS` is NOT the fix —
      see PRINTER_PROTOCOL.md.
- [ ] Print a photo (dithered) and a line drawing (threshold). *(Phase 4)*
- [x] Out-of-paper and lid-open surface an error. **Finding:** the printer sends the same `err:` code (2) for
      both, even with the lid closed, so the app says "Close the lid and check the paper".
- [ ] Low-battery warning surfaces. *(Not reproducible with a charged unit; threshold 7000 mV is provisional.)*
- [x] Disconnect (printer off / asleep) then auto-reconnect: link loss → "trying again" → reconnected ~10 s later.
- [ ] Print immediately after device wake from sleep.

## Static analysis
`./gradlew ktlintCheck detekt lint` + Kover coverage. Spotless ties formatting together.

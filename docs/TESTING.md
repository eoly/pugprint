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
- `:core:imaging`: assert dithered output bitmaps against checked-in PNGs.

## Hardware checklist (run on the real printer before each release)
- [ ] Pair via CDM on a fresh install (permissions prompt appears).
- [ ] Print full-black test row (verifies energy/density).
- [ ] Print a photo (dithered) and a line drawing (threshold).
- [ ] Out-of-paper and cover-open surface correct errors.
- [ ] Low-battery warning surfaces.
- [ ] Disconnect (walk away) then auto-reconnect.
- [ ] Print immediately after device wake from sleep.

## Static analysis
`./gradlew ktlintCheck detekt lint` + Kover coverage. Spotless ties formatting together.

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

## Golden tests (highest ROI)
- `:core:printer`: assert exact bytes for each command (frame, CRC8, LEN) against
  checked-in fixtures — this validates the protocol WITHOUT hardware.
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

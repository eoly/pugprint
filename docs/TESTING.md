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
Roborazzi goldens live in `app/screenshots/` and `ui/design/screenshots/` (committed; the
design-kit ones are one gallery per theme in `ThemeCatalog`, recorded automatically). They are recorded on macOS and
verified on Linux CI; the two Skia builds differ by a few dozen pixels in anti-aliased text and
nearest-neighbour image sampling, so `Screenshots.options` allows 0.05 % changed pixels
(≈ 1 600 px) — enough to absorb that, far below any real UI change.

## Accessibility
`AccessibilityAuditTest` (`:app`, Robolectric) renders every screen state and asserts each node
with a click action has text or a content description and bounds of at least 48 dp. It is the
accessibility lint for Compose; add a case when you add a screen. `Screenshots.BigText` wraps a
screen at font scale 1.5 for the `*_bigText` goldens, which catch clipped or truncated labels.

## Golden tests (highest ROI)
- `:core:printer`: `PrintJobGoldenTest` asserts `PrintJob.writes()` reproduces the vendor
  jobs in `core/printer/src/test/resources/print_job/*.vendor.hex` byte-for-byte, and
  `PrinterEmulatorTest` replays them back into the matching `.pbm` rows — this validates
  the protocol WITHOUT hardware. Regenerate fixtures with
  `python spike/bleak/vendor_print.py --dump <pattern>`.
- `:core:imaging`: `TestPatternTest` asserts the built-in test page against
  `core/imaging/src/test/resources/test_pattern.pbm`; `DitherTest` asserts Floyd–Steinberg and
  threshold output for `SyntheticPhoto` against `dither_photo.pbm` / `dither_drawing.pbm`.
  `TextRasterizerTest` records `font_<id>.pbm` (a pangram per `FontCatalog` font) and
  `StickerRendererTest` records `sticker_caption_bottom.pbm` / `sticker_caption_top.pbm` /
  `sticker_stamps.pbm`; `StampCatalogTest` records `stamp_<id>.pbm` per stamp; `StrokeRasterizerTest` records
  `drawing_face.pbm`; `StickerRollCatalogTest` records `roll_square_placement.pbm`.
  Re-record with `./gradlew :core:imaging:test -Dpugprint.recordGoldens=true` and eyeball the
  PBMs (any image viewer opens P1 PBM). `GrayImageTest`, `CropWindowTest` and
  `ImagePipelineTest` pin the rotate/crop/scale maths and the crop-frame geometry.

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
  reconnect, all on the fake transport. `EditorViewModelTest`: open → crop/rotate/shape →
  preview render → print, with a map-backed `PhotoSource` and a test dispatcher; the printed
  rows land in the emulator.

## Emulator (no Bluetooth)
`./gradlew installDebug -Ppugprint.fakePrinter=true` wires `FakePrinterTransport` and instant
pairing so the whole flow can be clicked through; the "printed" rows live in the emulator object.

## Hardware checklist (run on the real printer before each release)
Last run: **2026-09-22**, Pixel 10 Pro XL (Android 17) + Hello Blink `HB-0342` — all items below passed
unless marked.
- [x] Pair via CDM on a fresh install ("Nearby devices" prompt on Android 12+, then the system picker lists `HB-nnnn`).
      Picker found the printer within ~1 s of the printer advertising.
- [x] Home screen shows "HB-nnnn is ready" with a battery percentage (67 % ≈ 7.6 V).
- [x] Print the test page: black band edge to edge, 1-dot bars resolved, all five bands, no missing rows.
      MTU negotiated 248 (247 requested). If rows drop, lowering `PrintTiming.BLOCK_GAP_MILLIS` is NOT the fix —
      see PRINTER_PROTOCOL.md.
- [x] Print a photo (Photo style) and a line drawing (Drawing style) from the Photo Picker; check
      the crop frame matches what printed and the rotate button turns the sticker.
      Run 2026-09-22 on the Phase 4 build (#22): pick → fit → preview → print worked end to end at the
      default (medium) density.
- [x] Out-of-paper and lid-open surface an error. **Finding:** the printer sends the same `err:` code (2) for
      both, even with the lid closed, so the app says "Close the lid and check the paper".
- [x] A full photo prints with no light horizontal lines. Run 2026-09-22 on the Pixel: with
      pacing by deadline + a high-priority connection the lines that every earlier photo showed
      are gone (lines mid-print mean rows arrived late).
- [x] On the square sticker roll the picture lands centred on the label (run 2026-09-22 after
      the 365-dot placement; "works well").
- [ ] Low-battery warning surfaces. *(Not reproducible with a charged unit; threshold 7000 mV is provisional.)*
- [x] Disconnect (printer off / asleep) then auto-reconnect: link loss → "trying again" → reconnected ~10 s later.
- [ ] Print immediately after device wake from sleep.

## Static analysis
`./gradlew ktlintCheck detekt lint` + Kover coverage. Spotless ties formatting together.

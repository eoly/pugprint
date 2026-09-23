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
- **Phase 3 — BLE transport: ✅ DONE 2026-09-21 (hardware-verified on a Pixel 10 Pro XL + `HB-0342`).**
  `PrinterTransport` / `FakePrinterTransport` / `PrinterClient` in `:core:printer` (paced
  one-row-per-write sending, paper pre-flight, cover-open abort, progress); new
  `:core:bluetooth` with Kable `BleTransport` (MTU 247, oversize writes refused),
  `CompanionPairing` (CDM filtered on the UART service) and `BluetoothPermissions`
  (`BLUETOOTH_CONNECT` only — ADR 0006); `PrinterManager` (remembered printer,
  auto-reconnect with backoff, battery/cover state, test-page print) and the home screen
  states behind it; `TestPattern` test page in `:core:imaging`. Emulator flow via
  `-Ppugprint.fakePrinter=true`. Hardware run: CDM picker finds the printer, MTU 248
  negotiated, test page prints all five bands with no dropped rows at 20 ms pacing,
  auto-reconnect works. Finding: `err:` code 2 means lid open *or* paper out
  (see `docs/TESTING.md` § Hardware checklist).
- **Phase 4 — Image pipeline + UI: ✅ DONE 2026-09-21, hardware-verified 2026-09-22 (photo and
  drawing printed from the Photo Picker on the Pixel + `HB-0342`).** `:core:imaging` gains `GrayImage` (8-bit luma, rotate / crop / box-filter
  scale), `Dither` (Floyd–Steinberg for photos, threshold for drawings, golden PBMs),
  `CropWindow` (the pure pan/zoom/shape maths behind the crop frame) and `ImagePipeline`
  (crop → 384 px → dither, capped at 1152 rows). App: system Photo Picker (no storage
  permission), `ContentResolverPhotoSource` (downsized decode, EXIF orientation), a two-step
  editor — "Make it fit" (pinch/drag, Square / Tall / Wide / Whole, rotate) then "Ready to
  print?" (the actual dots, Photo / Drawing style, Print) — `PrinterManager.printImage`,
  navigation-compose, ViewModel tests and Roborazzi goldens for seven editor states.
- **Phase 5 — Polish + kid features: ✅ DONE 2026-09-22 (tasks 1–9 merged as #24–#33; hardware-verified
  on the Pixel + `HB-0342`, including the sticker placement and the pacing fix for light lines).** Built so a later
  "designer" phase can change themes, layout and add fun content without touching the printing
  core (ADR 0007). PR-sized tasks, each independently testable:
  1. ✅ **Design kit** — `:ui:design`: theme tokens, `ThemeCatalog` (Pug / Bubblegum / Ocean),
     `KidScreen` / `BigButton` / `ChoiceRow` / `StatusBanner`, WCAG contrast tests over the
     catalog, one gallery golden per theme; home + editor rebuilt on the kit;
     `docs/DESIGN_KIT.md`.
  2. ✅ **Settings + theme picker + darkness** — `SettingsStore` (theme id, `DensityLevel`) in
     app-private preferences, "Pick a look" `ThemePicker` on the home screen (each tile in its
     own colours), "How dark?" Lighter / Normal / Darker on the preview step,
     `PrinterManager.printImage(bitmap, density)`; density asserted per level against the
     emulator.
  3. ✅ **Friendly error UX + print again** — every message and offline reason is a
     `StatusBanner` with kid-readable words plus a "what to do" hint (`MessageBanner.kt`,
     `printerStatusHint`; the snackbar is gone), "Print it again" repeats the last sticker
     (`PrinterManager.lastPrint` / `printAgain`), printing shows "40% done".
  4. ✅ **Sticker document + text captions** — `:core:imaging` `Sticker` (picture + crop + style +
     `Caption`) rendered by `StickerRenderer` onto a `BitCanvas` (golden PBMs); `PixelFont` /
     `FontCatalog` (5 × 7 "Blocky", glyphs drawn as `#`/`.` art, add a font = add an entry) and
     `TextRasterizer` (wrap to 3 lines, auto-scale 6→2, chop giant words); "Add words" detour
     from the preview with live dots, Top / Bottom; `BigTextField` in the kit.
  5. ✅ **Stamps** — `StampCatalog` (fourteen 16 × 16 stamps drawn as `#`/`.` art, add a stamp = add an
     entry), `StampPlacement` on the `Sticker` (centre as fractions, Small / Medium / Big),
     `StampRasterizer` with a white halo; "Add stamps" detour: tap a stamp, it lands in the
     middle, drag it into place, Undo. Goldens per stamp and for a stamped sticker.
  6. ✅ **Drawing canvas** — `Drawing` / `Stroke` / `BrushSize` and a pure `StrokeRasterizer`
     (round-capped strokes as discs, eraser paints white; golden `drawing_face.pbm`); "Draw a
     sticker" on the home screen → a square sheet with Thin / Medium / Fat, Pen / Eraser, Undo,
     Start over; Next (also on a blank sheet, for words-and-stamps-only stickers) hands the
     384 × 384 picture to the editor through `DrawingHandoff`, which skips the crop step for it;
     words, stamps, darkness and printing all work on drawings unchanged.
  7. ✅ **Accessibility pass** — every tappable thing has words for TalkBack and is ≥ 48 dp,
     enforced by `AccessibilityAuditTest` (walks the semantics tree of nine screen states;
     Compose has no lint for this). Banners read as one item and announce themselves
     (`StatusBanner.announce`), titles are headings, the crop frame and stamp canvas are
     described, stamp tiles are buttons. 1.5× font-scale goldens for home / preview / draw
     found two bugs, fixed: home now scrolls, tile labels auto-shrink instead of truncating.
  8. ✅ **Sticker rolls** — `StickerRoll` / `StickerRollCatalog` in `:core:imaging` (one entry
     per roll: label size, gap, `PrintPlacement`). "Square stickers" is the standard roll
     measured 2026-09-22 (49.2 mm square, 12.7 mm gap; a full print landed 3.2 mm from the
     left, 0.8 mm from the right, flush top, 3.2 mm short at the bottom), so its picture is
     365 dots square placed with 19 white rows on top and 19 dots on the right; "Plain roll"
     is untouched 58 mm paper. The roll is a setting ("Which stickers are in the printer?"
     on home); the editor renders at the roll's width and row cap, locks the shape to Square
     for labels, and `place()`s the dots onto the head canvas before printing. The firmware
     feeds to the serration itself, so `:core:printer` is unchanged. Golden
     `roll_square_placement.pbm`.
  9. ✅ **Designer handbook + gallery screen** — `docs/DESIGN_KIT.md` has a "Start here"
     walkthrough and recipes for themes, fonts, stamps, rolls, brushes, words and components;
     debug builds show a **Design gallery** button on home (`GalleryRoute`, every kit component
     in any theme, preview only).
  10. ✅ **Round stickers** — `LabelShape` on a roll's label; "Round stickers" (`circle-49`, 49.2 mm on the
     square roll's liner; hardware-tuned 2026-09-22 over five prints to a 356-dot circle with
     no top margin, 18 dots free left / 10 right — the printer starts a round label 1/16–1/8 in
     below its top and the head cannot start higher, so a slightly smaller circle is centred) renders through `StickerRoll.render`,
     which clips the dots to the inscribed circle and fits the caption as a cap inside the
     curve. Crop frame, preview and draw sheet all show the circle.
- **Phase 5b — Coloring pages** (ADR 0008): pre-made outline pictures a kid prints and colours in
  with crayons. Each task is one PR.
  1. ✅ **Catalog + goldens** — `ColoringPage` / `ColoringPageCatalog` in `:core:imaging` (twelve pages:
     pug, heart, star, cat, sun, flower, rainbow, ice cream, cupcake, fish, butterfly, rocket) drawn with the `Outline` builder (`circle`,
     `ellipse`, `arc`, `path`, `loop`, `star`, `dot` in page fractions; `BrushSize` picks the line
     weight); a page is a `Drawing`, rendered by `StrokeRasterizer`. `ColoringPageCatalogTest`
     enumerates the catalog: ids, names, outlines only (3–30 % ink), all ink inside
     `SAFE_RADIUS` so the page prints whole on round labels, a golden `coloring_<id>.pbm` each.
     Handbook recipe "Add a coloring page".
  2. ✅ **Picker screen** — "Color a picture" on home → `ColoringRoute` / `ColoringViewModel` /
     `ColoringScreen`: the pages two to a row as big tiles (preview + name, the round guide on a
     round roll); tapping one renders it into `DrawingHandoff` and opens the editor on the
     preview in Drawing style, so words, stamps and the roll all work unchanged. Roborazzi
     goldens (+ `_bigText`, round roll), a11y audit (now measures a tile's laid-out size, so a
     scrolled-off tile counts), ViewModel test.
  3. **Colour it on the tablet** — a page can open on the draw sheet with its outline already
     there (`DrawViewModel` takes a starting `Drawing`; Undo stops at the outline), for kids
     who want to add their own lines before printing.
  4. **More pages** — designer session with the handbook recipe; a page is ~10 lines of Kotlin
     and a recorded golden.
- **Phase 6 — Play:** internal track to the friend group → (if going public) closed test
  (12 testers/14 days) → production.

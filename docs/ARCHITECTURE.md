# Architecture

## Layers
- **UI**: Jetpack Compose screens + ViewModels exposing immutable UI state via StateFlow (UDF).
  Screens are built only from `:ui:design` components and tokens — never raw Material widgets
  or `dp` literals — so the look and layout change in the kit, not per screen.
  `HomeRoute` (in `ui/home/HomeRoute.kt`) owns the platform glue a ViewModel cannot: the runtime
  permission prompt and the Companion Device Manager picker (`IntentSender`).
  `HomeRoute` also launches the system Photo Picker; `EditorRoute` owns the editor's
  back-stack behaviour: Back returns to where the picture came from, while a finished print and
  the header's Home button pop to `home` (progress and "Print it again" live there). `PugPrintNavHost` (navigation-compose, string routes) holds three
  screens: `home`, `draw` and `editor/{photo}`. `DrawRoute` / `DrawViewModel` keep a `Drawing`
  (strokes as fractions of the sheet) and, on Next, rasterise it into `DrawingHandoff`; the
  editor opens `DrawingHandoff.URI` like any picture via `HandoffPhotoSource`.
- **Domain**: `PrinterManager` — the app-wide connection state machine (pair, connect,
  identify, reconnect with backoff, `printImage`). `EditorViewModel` drives the edit:
  `PhotoSource` → `GrayImage` → `CropWindow` (pan/zoom/shape/rotate) → `Sticker` (+ caption)
  → `StickerRenderer` → `MonoBitmap` → `PrinterManager.printImage`.
- **Data**: `PrinterTransport` implementations, `PairedPrinterStore`, `SettingsStore`
  (`AppSettings`: theme id + print density, app-private preferences, exposed as a `StateFlow`
  that `MainActivity` maps to the theme and the ViewModels fold into their state),
  `ContentResolverPhotoSource` (decodes a picked picture to ≤ 1600 px luma, EXIF-corrected).

## Modules
- `:app`            — Compose UI, ViewModels, `PrinterManager`, Hilt wiring, manifest/permissions.
- `:core:printer`   — PURE JVM: ESC/POS command encoders, `GS v 0` raster blocks, `PrintJob`
  sequencing/pacing, reply parsers, `CommandDecoder` + `PrinterEmulator`, and the transport
  abstraction (`PrinterTransport`, `FakePrinterTransport`, `PrinterClient`). Golden tests
  (see `docs/PRINTER_PROTOCOL.md`).
- `:core:imaging`   — PURE JVM: `GrayImage` (rotate / crop / box-filter scale), `Dither`
  (Floyd–Steinberg, threshold), `CropWindow` (crop-frame maths), `ImagePipeline`, `MonoBitmap`
  1bpp packing, `TestPattern`; the sticker document — `Sticker` (picture + crop + style +
  `Caption`), `StickerRenderer` (pipeline → `BitCanvas` → overlays → dots), `PixelFont` /
  `FontCatalog` / `TextRasterizer`, `StampCatalog` / `StampRasterizer`, `Drawing` / `StrokeRasterizer`; golden PBM tests.
- `:core:bluetooth` — Android: Kable `BleTransport`, `CompanionPairing` (CDM), `BluetoothPermissions`.
  The only module allowed to import Android Bluetooth APIs; no protocol bytes (ADR 0006).
- `:ui:design`     — the design kit (ADR 0007): theme tokens (`PugTheme`, `PugSpacing`,
  `PugTouch`, `PugLayout`), the `ThemeCatalog`, `PugPrintTheme`, and the kid-sized components
  every screen is built from (`KidScreen`, `HeroTitle`, `BigButton`, `ChoiceRow`,
  `StatusBanner`). Compose only; depends on no other module. Contrast tests and one gallery
  golden per theme (`ui/design/screenshots/`). Designer handbook: `docs/DESIGN_KIT.md`.
- Editor UI lives in `:app` under `ui/editor` (`EditorScreen`, `CropFrame`, `EditorViewModel`);
  a `:feature:editor` module (ADR 0004) is deferred until the stamps/drawing work in Phase 5
  makes it worth the split.

## Printer transport abstraction
```kotlin
interface PrinterTransport {
    val state: StateFlow<TransportState>          // Disconnected(cause) / Connecting / Connected / Disconnecting
    fun notifications(): Flow<ByteArray>           // UART TX notifications, hot for the life of the connection
    suspend fun connect(device: PrinterDevice)     // throws TransportException
    suspend fun write(bytes: ByteArray)            // exactly one link-layer write; refuses oversize payloads
    suspend fun disconnect()
}

// JVM: wraps :core:printer's PrinterEmulator (keeps printed rows, flags BLE-rule violations).
class FakePrinterTransport(emulator: PrinterEmulator = PrinterEmulator()) : PrinterTransport

// Android: Kable Peripheral on the Microchip transparent-UART service (HelloBlinkUuids).
class BleTransport(peripherals: PeripheralFactory, scope: CoroutineScope) : PrinterTransport

// Protocol operations over any transport, serialised by a mutex.
class PrinterClient(transport: PrinterTransport) {
    suspend fun identify(): PrinterIdentity                       // status / serial / product → DensityProfile, battery
    suspend fun print(job: PrintJob, onProgress: (PrintProgress) -> Unit): PrintResult
    val replies: Flow<PrinterReply>                               // decoded notifications (err:, LABELOK …)
}
```
`PrinterClient.print` sends `PrintJob.writes()` in order, one BLE write per `PrinterWrite`,
delaying `pauseAfterMillis` after each (1 row per `GS v 0` block, 20 ms apart, 250 ms before
the feed — ADR 0005). It runs a `GS r 1` paper pre-flight first and aborts on `err:` cover-open
or link loss, returning `PrintResult.Failure(reason, progress)` instead of throwing.

`BleTransport` requests MTU 247 on service discovery and refuses any write larger than the
negotiated payload, so a block can never be fragmented across writes. It observes TX for the
whole connection and relays into a hot `SharedFlow`, so a query's reply cannot be missed.

## Pairing and permissions (ADR 0006)
Companion Device Manager association filtered on the UART service UUID → the picked device's
address is stored → Kable connects by address. Permissions: `BLUETOOTH_CONNECT` (Android 12+)
and legacy `BLUETOOTH` (≤ 30). No scan or location permission.

## Image pipeline
`ContentResolverPhotoSource` decodes on `Dispatchers.IO` with `ImageDecoder` (API 28+, applies
EXIF itself) or `BitmapFactory` + `ExifInterface` (API 26–27), sized to fit 1600 px, and reads
the pixels row by row into a `GrayImage` (`(38R + 75G + 15B) >> 7`, the vendor's weighting).
`CropWindow` models the crop frame in *frame widths* so the ViewModel never sees screen
pixels: the picture covers a frame of the chosen `CropShape`, zoom ∈ [1, 4], pans clamped
so the frame is always full; `cropRect()` maps the frame back to image pixels. `CropFrame`
(Compose) only converts gesture pixels to frame widths and draws. `ImagePipeline.render`
crops, box-filters to 384 px, trims to at most 1152 rows, and dithers (`DitherMode.PHOTO` =
Floyd–Steinberg, `DRAWING` = threshold at 128). Heavy steps run on the injected
`@ImagingDispatcher` (`Dispatchers.Default`; a test dispatcher in tests).

## Sticker document
`Sticker` is the editable thing: the picture, its crop and dither style, plus layers drawn on
top, bottom to top: `StampPlacement`s (a `StampCatalog` stamp centred at fractions of the
sticker, at a `StampSize`; `StampRasterizer` paints a one-dot white halo first so it reads
over a photo) and then a `Caption` (a white band with black `PixelFont` letters at the top or bottom;
`TextRasterizer` wraps to ≤ 3 lines and picks the largest integer scale 6→2 that fits, chopping
a word that never fits). `StickerRenderer.render` runs `ImagePipeline`, lifts the dots into a
`BitCanvas`, draws the layers and packs them back. A drawing is not a layer but a picture: `StrokeRasterizer` turns `Drawing` strokes into a
black-on-white `GrayImage` that enters the same pipeline, so captions and stamps work on it.
`:core:printer` never changes for a new kid feature (ADR 0007).

## Threading
BLE and encoding on `Dispatchers.Default`/Kable's own threads; the `PrinterManager` lives in
an application-scoped `CoroutineScope`. `PrinterClient` is a single-consumer operation queue
(`Mutex`) that serialises and paces GATT writes — write-without-response has no ACK.

## Error handling
`PrintResult.Failure(reason)` with `DISCONNECTED`, `NO_PAPER`, `LID_OR_PAPER`, `PRINTER_ERROR`,
`WRITE_FAILED`. `PrinterIdentity.batteryLow` from `VOLT=`. Lid-open / paper-out (one `err:` code on this firmware) tracked live from
`err:` notifications. `PrinterManager` reconnects with exponential backoff (1 s → 30 s cap). The home screen turns
every outcome into a `StatusBanner` with a hint (`HomeMessage.banner()` in `ui/home/MessageBanner.kt`,
`printerStatusHint`); `PrinterManager.lastPrint` remembers the last sticker for "Print it again".

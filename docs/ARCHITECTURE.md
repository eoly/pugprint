# Architecture

## Layers
- **UI**: Jetpack Compose screens + ViewModels exposing immutable UI state via StateFlow (UDF).
  `HomeRoute` (in `MainActivity.kt`) owns the platform glue a ViewModel cannot: the runtime
  permission prompt and the Companion Device Manager picker (`IntentSender`).
- **Domain**: `PrinterManager` — the app-wide connection state machine (pair, connect,
  identify, reconnect with backoff, print). Later: `PrintImageUseCase`.
- **Data**: `PrinterTransport` implementations, `PairedPrinterStore`, imaging pipeline.

## Modules
- `:app`            — Compose UI, ViewModels, `PrinterManager`, Hilt wiring, manifest/permissions.
- `:core:printer`   — PURE JVM: ESC/POS command encoders, `GS v 0` raster blocks, `PrintJob`
  sequencing/pacing, reply parsers, `CommandDecoder` + `PrinterEmulator`, and the transport
  abstraction (`PrinterTransport`, `FakePrinterTransport`, `PrinterClient`). Golden tests
  (see `docs/PRINTER_PROTOCOL.md`).
- `:core:imaging`   — PURE JVM: dithering, scaling to 384 px, 1bpp packing, `TestPattern`, golden tests.
- `:core:bluetooth` — Android: Kable `BleTransport`, `CompanionPairing` (CDM), `BluetoothPermissions`.
  The only module allowed to import Android Bluetooth APIs; no protocol bytes (ADR 0006).
- `:feature:editor` — (Phase 4) crop/rotate/preview/stamps UI.

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

## Threading
BLE and encoding on `Dispatchers.Default`/Kable's own threads; the `PrinterManager` lives in
an application-scoped `CoroutineScope`. `PrinterClient` is a single-consumer operation queue
(`Mutex`) that serialises and paces GATT writes — write-without-response has no ACK.

## Error handling
`PrintResult.Failure(reason)` with `DISCONNECTED`, `NO_PAPER`, `COVER_OPEN`, `PRINTER_ERROR`,
`WRITE_FAILED`. `PrinterIdentity.batteryLow` from `VOLT=`. Cover-open tracked live from
`err:` notifications. `PrinterManager` reconnects with exponential backoff (1 s → 30 s cap).

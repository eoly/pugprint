# Architecture

## Layers
- **UI**: Jetpack Compose screens + ViewModels exposing immutable UI state via StateFlow (UDF).
- **Domain**: use cases (e.g., `PrintImageUseCase`, `ConnectPrinterUseCase`).
- **Data**: `PrinterRepository`, transport implementations, imaging pipeline.

## Modules
- `:app`            — Compose UI, ViewModels, DI wiring, Android manifest/permissions.
- `:core:printer`   — PURE JVM: ESC/POS command encoders, `GS v 0` raster blocks, `PrintJob`
  sequencing/pacing, reply parsers, `CommandDecoder` + `PrinterEmulator`, golden tests
  (see `docs/PRINTER_PROTOCOL.md`).
- `:core:imaging`   — PURE JVM: dithering, scaling to 384 px, 1bpp packing, golden tests.
- `:core:bluetooth` — Android: Kable-based `BleTransport`, CDM pairing, permission helpers.
- `:feature:editor` — crop/rotate/preview/stamps UI.

## Printer transport abstraction (sketch)
```kotlin
interface PrinterTransport {
    val state: StateFlow<TransportState>
    suspend fun connect(device: PrinterDevice)
    suspend fun write(bytes: ByteArray)        // one PrinterWrite → write-without-response on the UART RX characteristic
    fun notifications(): Flow<ByteArray>        // notify on the UART TX characteristic → PrinterReply.parse
    suspend fun disconnect()
}

// Emulator/JVM implementation: wraps :core:printer's PrinterEmulator, which keeps the received
// rows (render to a Bitmap for preview) and flags BLE-rule violations in tests.
class FakePrinterTransport(private val emulator: PrinterEmulator) : PrinterTransport { /* ... */ }

// Real device: Kable Peripheral wrapping the Microchip transparent-UART service (HelloBlinkUuids).
class BleTransport(/* Kable Peripheral */) : PrinterTransport { /* ... */ }
```
The transport sends `PrintJob.writes()` in order, one BLE write per `PrinterWrite`, sleeping
`pauseAfterMillis` after each (1 row per `GS v 0` block, ~20 ms apart — see ADR 0005).

## Threading
BLE and encoding on `Dispatchers.IO`. A single-consumer operation queue (Channel/Mutex)
serializes GATT writes and paces them (write-without-response has no ACK — pace to avoid
overrunning the printer buffer).

## Error handling
`sealed interface PrintResult { data object Success; data class Failure(reason) }`.
Surface `NoPaper`, `CoverOpen`, `LowBattery`, `Disconnected`, `Timeout` from the notify
characteristic (`PrinterReply.Error`, `EscPosStatus`, `Status.batteryMillivolts`).
Auto-reconnect with exponential backoff.

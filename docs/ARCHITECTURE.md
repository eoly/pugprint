# Architecture

## Layers
- **UI**: Jetpack Compose screens + ViewModels exposing immutable UI state via StateFlow (UDF).
- **Domain**: use cases (e.g., `PrintImageUseCase`, `ConnectPrinterUseCase`).
- **Data**: `PrinterRepository`, transport implementations, imaging pipeline.

## Modules
- `:app`            — Compose UI, ViewModels, DI wiring, Android manifest/permissions.
- `:core:printer`   — PURE JVM: protocol frames, CRC8, row encoders, command catalogue, golden tests.
- `:core:imaging`   — PURE JVM: dithering, scaling to 384 px, 1bpp packing, golden tests.
- `:core:bluetooth` — Android: Kable-based `BleTransport`, CDM pairing, permission helpers.
- `:feature:editor` — crop/rotate/preview/stamps UI.

## Printer transport abstraction (sketch)
```kotlin
interface PrinterTransport {
    val state: StateFlow<TransportState>
    suspend fun connect(device: PrinterDevice)
    suspend fun write(bytes: ByteArray)        // maps to write-no-response on 0xae01
    fun notifications(): Flow<ByteArray>        // maps to notify on 0xae02
    suspend fun disconnect()
}

// Emulator/JVM implementation: renders received image rows into a Bitmap for preview & tests.
class FakePrinterTransport(private val width: Int = 384) : PrinterTransport { /* ... */ }

// Real device: Kable Peripheral wrapping the BLE GATT (service 0xae30).
class BleTransport(/* Kable Peripheral */) : PrinterTransport { /* ... */ }
```

## Threading
BLE and encoding on `Dispatchers.IO`. A single-consumer operation queue (Channel/Mutex)
serializes GATT writes and paces them (write-without-response has no ACK — pace to avoid
overrunning the printer buffer).

## Error handling
`sealed interface PrintResult { data object Success; data class Failure(reason) }`.
Surface `NoPaper`, `LowBattery`, `Overheated`, `Disconnected`, `Timeout` from the notify
characteristic (0xa3 status responses). Auto-reconnect with exponential backoff.

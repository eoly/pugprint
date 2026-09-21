package com.example.pugprint.printer.transport

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * A printer the transport can reach.
 *
 * @property id transport-specific address — the BLE MAC on Android. Learned at pairing time
 *   (Companion Device Manager, filtered on [com.example.pugprint.printer.HelloBlinkUuids.SERVICE]);
 *   never hardcoded.
 * @property name advertised name such as `HB-1234`, for display only.
 */
public data class PrinterDevice(
    val id: String,
    val name: String? = null,
) {
    /** What to show a user: the advertised name, else the address. */
    val displayName: String get() = name ?: id
}

/** Why a transport is [TransportState.Disconnected]. */
public enum class DisconnectCause {
    /** Never connected, or disconnected because [PrinterTransport.disconnect] was called. */
    NONE,

    /** The link dropped without a local request — printer off, out of range, or asleep. */
    LOST,

    /** A [PrinterTransport.connect] attempt did not reach the connected state. */
    FAILED,
}

/** Link state of a [PrinterTransport]. */
public sealed interface TransportState {
    public data class Disconnected(
        val cause: DisconnectCause = DisconnectCause.NONE,
    ) : TransportState

    public data class Connecting(
        val device: PrinterDevice,
    ) : TransportState

    public data class Connected(
        val device: PrinterDevice,
    ) : TransportState

    public data class Disconnecting(
        val device: PrinterDevice,
    ) : TransportState
}

/** Raised by a [PrinterTransport] when a connect or write cannot be carried out. */
public open class TransportException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Byte pipe to one printer. All printer I/O in the app goes through this interface so
 * the protocol layer and the UI can run against [FakePrinterTransport] without hardware.
 *
 * Implementations must send each [write] as exactly one link-layer write — a `GS v 0`
 * block split across writes is discarded by the printer (docs/PRINTER_PROTOCOL.md).
 */
public interface PrinterTransport {
    public val state: StateFlow<TransportState>

    /**
     * Payloads the printer sends back (the UART TX characteristic). Hot: notifications that
     * arrive while nobody collects are lost, so subscribe before sending a query. The flow
     * does not complete on disconnect; it resumes with the next connection.
     */
    public fun notifications(): Flow<ByteArray>

    /** Connects to [device], suspending until [TransportState.Connected]. Throws [TransportException] on failure. */
    public suspend fun connect(device: PrinterDevice)

    /** Sends [bytes] in a single write. Throws [TransportException] when not connected or [bytes] is too large. */
    public suspend fun write(bytes: ByteArray)

    /** Drops the link, or cancels an in-flight [connect]. Safe to call when already disconnected. */
    public suspend fun disconnect()
}

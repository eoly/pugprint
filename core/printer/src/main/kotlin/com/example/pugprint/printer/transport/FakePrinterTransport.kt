package com.example.pugprint.printer.transport

import com.example.pugprint.printer.PrinterEmulator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory [PrinterTransport] backed by a [PrinterEmulator]: every write is decoded and
 * "printed" into [PrinterEmulator.rows], and the emulator's replies come back through
 * [notifications]. Enforces the same size limit as one BLE write so a job that works here
 * also fits the hardware. Used by the app's fake-printer build and by unit tests.
 *
 * @property maxWriteBytes largest payload accepted per write; defaults to the 245-byte
 *   payload of the MTU negotiated in the spike.
 */
public class FakePrinterTransport(
    public val emulator: PrinterEmulator = PrinterEmulator(),
    public val maxWriteBytes: Int = DEFAULT_MAX_WRITE_BYTES,
) : PrinterTransport {
    private val mutableState = MutableStateFlow<TransportState>(TransportState.Disconnected())
    private val replies = MutableSharedFlow<ByteArray>(extraBufferCapacity = REPLY_BUFFER)
    private val writeLog = ArrayList<ByteArray>()

    override val state: StateFlow<TransportState> = mutableState.asStateFlow()

    /** Every payload written since construction, in order. */
    public val writes: List<ByteArray> get() = writeLog

    /** When true, [connect] fails the way an absent printer would. */
    public var connectFails: Boolean = false

    /** When set, the link drops ([DisconnectCause.LOST]) as soon as this many writes have been accepted. */
    public var dropAfterWrites: Int? = null

    override fun notifications(): Flow<ByteArray> = replies

    override suspend fun connect(device: PrinterDevice) {
        mutableState.value = TransportState.Connecting(device)
        if (connectFails) {
            mutableState.value = TransportState.Disconnected(DisconnectCause.FAILED)
            throw TransportException("fake printer ${device.displayName} is not answering")
        }
        mutableState.value = TransportState.Connected(device)
    }

    override suspend fun write(bytes: ByteArray) {
        if (state.value !is TransportState.Connected) throw TransportException("not connected")
        if (bytes.size > maxWriteBytes) {
            throw TransportException("write of ${bytes.size} B exceeds the $maxWriteBytes B link payload")
        }
        writeLog += bytes.copyOf()
        emulator.write(bytes).forEach { replies.emit(it) }
        if (writeLog.size == dropAfterWrites) dropConnection()
    }

    override suspend fun disconnect() {
        mutableState.value = TransportState.Disconnected(DisconnectCause.NONE)
    }

    /** Simulates the printer going away (switched off, out of range). */
    public fun dropConnection() {
        mutableState.value = TransportState.Disconnected(DisconnectCause.LOST)
    }

    /** Injects an unsolicited notification such as `err:.` (cover open). */
    public suspend fun notify(payload: ByteArray) {
        replies.emit(payload)
    }

    public companion object {
        /** Write payload for the 248-byte MTU negotiated in the Phase 0 spike (MTU − 3). */
        public const val DEFAULT_MAX_WRITE_BYTES: Int = 245
        private const val REPLY_BUFFER = 64
    }
}

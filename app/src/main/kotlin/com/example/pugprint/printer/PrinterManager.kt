package com.example.pugprint.printer

import com.example.pugprint.imaging.MonoBitmap
import com.example.pugprint.imaging.TestPattern
import com.example.pugprint.printer.transport.PrintProgress
import com.example.pugprint.printer.transport.PrintResult
import com.example.pugprint.printer.transport.PrinterClient
import com.example.pugprint.printer.transport.PrinterDevice
import com.example.pugprint.printer.transport.PrinterIdentity
import com.example.pugprint.printer.transport.TransportException
import com.example.pugprint.printer.transport.TransportState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** Why a paired printer is not connected right now. */
enum class OfflineReason {
    /** Connecting has not been attempted (permission missing) or the user disconnected. */
    IDLE,

    /** The link dropped; a reconnect is scheduled. */
    LOST,

    /** The last connect attempt failed; another is scheduled. */
    UNREACHABLE,

    /** Android 12+ Bluetooth permission not granted; connecting is on hold. */
    NO_PERMISSION,
}

/** The printer as the app sees it. */
sealed interface PrinterState {
    data object NoPrinter : PrinterState

    data class Offline(
        val device: PrinterDevice,
        val reason: OfflineReason,
    ) : PrinterState

    data class Connecting(
        val device: PrinterDevice,
        val attempt: Int,
    ) : PrinterState

    data class Connected(
        val device: PrinterDevice,
        val identity: PrinterIdentity,
        /** `err:` code 2 is active: lid open or out of paper. */
        val paperOrLidProblem: Boolean = false,
    ) : PrinterState

    data class Printing(
        val device: PrinterDevice,
        val identity: PrinterIdentity,
        val progress: PrintProgress,
    ) : PrinterState
}

/** Reconnect schedule: delay in ms before attempt `n` (1-based). */
fun interface ReconnectBackoff {
    fun delayMillis(attempt: Int): Long

    companion object {
        private const val BASE_MILLIS = 1_000L
        private const val CAP_MILLIS = 30_000L
        private const val MAX_DOUBLINGS = 5

        /** 1 s, 2 s, 4 s, 8 s, 16 s, then 30 s. */
        val EXPONENTIAL: ReconnectBackoff =
            ReconnectBackoff { attempt ->
                val doublings = (attempt - 1).coerceIn(0, MAX_DOUBLINGS)
                (BASE_MILLIS shl doublings).coerceAtMost(CAP_MILLIS)
            }
    }
}

/**
 * Owns the connection to the paired printer for the life of the app: remembers the device,
 * connects on launch, identifies the printer (battery, density table), reconnects with
 * backoff when the link drops, tracks lid/paper errors, and runs print jobs.
 */
@Singleton
class PrinterManager
    @Inject
    constructor(
        private val client: PrinterClient,
        private val store: PairedPrinterStore,
        private val permission: ConnectPermission,
        @ApplicationScope private val scope: CoroutineScope,
        private val backoff: ReconnectBackoff,
    ) {
        private val mutableState = MutableStateFlow<PrinterState>(PrinterState.NoPrinter)
        private val mutablePrintResult = MutableStateFlow<PrintResult?>(null)
        private val printLock = Mutex()
        private var connection: Job? = null

        val state: StateFlow<PrinterState> = mutableState.asStateFlow()

        /** Outcome of the last print, until [dismissPrintResult]. */
        val lastPrintResult: StateFlow<PrintResult?> = mutablePrintResult.asStateFlow()

        /** Call once at startup: reconnects to the remembered printer if allowed. */
        fun start() {
            if (state.value == PrinterState.NoPrinter) retry()
        }

        /** Remembers [device] and (re)starts the connection loop for it. */
        fun connect(device: PrinterDevice) {
            store.save(device)
            connection?.cancel()
            connection = scope.launch { maintainConnection(device) }
        }

        /** Connects to the remembered printer now (e.g. after the permission was granted). */
        fun retry() {
            val device = store.load() ?: return
            if (permission.isGranted()) {
                connect(device)
            } else {
                mutableState.value = PrinterState.Offline(device, OfflineReason.NO_PERMISSION)
            }
        }

        /** Drops the link and forgets the printer. */
        fun forget() {
            connection?.cancel()
            connection = null
            store.clear()
            mutableState.value = PrinterState.NoPrinter
            scope.launch { client.disconnect() }
        }

        /**
         * Prints [bitmap] (one row per raster block, medium density for this printer's firmware);
         * the result lands in [lastPrintResult]. Ignored unless the printer is connected and idle.
         */
        fun printImage(bitmap: MonoBitmap) {
            require(bitmap.width == PrinterSpec.DOTS_PER_LINE) {
                "Print width must be ${PrinterSpec.DOTS_PER_LINE} dots, got ${bitmap.width}"
            }
            scope.launch {
                val connected = state.value as? PrinterState.Connected ?: return@launch
                print(printJobFor(bitmap, connected.identity.densityProfile))
            }
        }

        fun dismissPrintResult() {
            mutablePrintResult.value = null
        }

        private suspend fun print(job: PrintJob) =
            printLock.withLock {
                val connected = state.value as? PrinterState.Connected ?: return@withLock
                val result =
                    client.print(job) { progress ->
                        mutableState.update { current ->
                            when (current) {
                                is PrinterState.Connected, is PrinterState.Printing ->
                                    PrinterState.Printing(connected.device, connected.identity, progress)
                                else -> current
                            }
                        }
                    }
                mutableState.update { current ->
                    if (current is PrinterState.Printing) connected.copy() else current
                }
                mutablePrintResult.value = result
            }

        /** Connect → identify → hold until the link drops → back off → repeat. Cancelled by [forget]/[connect]. */
        private suspend fun maintainConnection(device: PrinterDevice) {
            var attempt = 1
            while (true) {
                mutableState.value = PrinterState.Connecting(device, attempt)
                val identity = tryConnect(device)
                if (identity != null) {
                    attempt = 1
                    mutableState.value = PrinterState.Connected(device, identity)
                    holdWhileConnected()
                    mutableState.value = PrinterState.Offline(device, OfflineReason.LOST)
                } else {
                    mutableState.value = PrinterState.Offline(device, OfflineReason.UNREACHABLE)
                }
                delay(backoff.delayMillis(attempt))
                attempt++
            }
        }

        private suspend fun tryConnect(device: PrinterDevice): PrinterIdentity? =
            try {
                client.connect(device)
                client.identify()
            } catch (_: TransportException) {
                client.disconnect()
                null
            }

        /** Tracks lid/paper error notifications until the transport reports a disconnect. */
        private suspend fun holdWhileConnected() {
            val coverWatch =
                scope.launch {
                    client.replies.filterIsInstance<PrinterReply.Error>().collect { error ->
                        mutableState.update { current ->
                            if (current is PrinterState.Connected) {
                                current.copy(paperOrLidProblem = error.kind == PrinterReply.ErrorKind.LID_OR_PAPER)
                            } else {
                                current
                            }
                        }
                    }
                }
            try {
                client.state.first { it is TransportState.Disconnected }
            } finally {
                coverWatch.cancel()
            }
        }
    }

/** One row per raster block at medium density, plus the speed command old private-factory firmware needs. */
private fun printJobFor(
    bitmap: MonoBitmap,
    profile: DensityProfile,
): PrintJob =
    PrintJob(
        rows = List(bitmap.height, bitmap::row),
        density = profile.value(DensityLevel.MEDIUM),
        options = PrintOptions(speed = profile.legacySpeed),
    )

/** Prints the built-in test page; the result lands in [PrinterManager.lastPrintResult]. */
fun PrinterManager.printTestPage(): Unit = printImage(TestPattern.render())

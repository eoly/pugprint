package com.example.pugprint.printer.transport

import com.example.pugprint.printer.DensityProfile
import com.example.pugprint.printer.EscPosStatus
import com.example.pugprint.printer.FactoryType
import com.example.pugprint.printer.PrintJob
import com.example.pugprint.printer.PrinterQueries
import com.example.pugprint.printer.PrinterReply
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

/** What a connected printer reported about itself (docs/PRINTER_PROTOCOL.md § Query / status commands). */
public data class PrinterIdentity(
    val status: PrinterReply.Status,
    /** `null` when the printer did not answer the serial query in time. */
    val serial: PrinterReply.Serial?,
    /** `null` when the printer did not answer the product query in time. */
    val product: PrinterReply.Product?,
) {
    /** Firmware table for this unit; a printer that will not say its product id is treated as a public-factory unit. */
    val densityProfile: DensityProfile =
        DensityProfile.forPrinter(product?.factory ?: FactoryType.PUBLIC, status.firmwareNumber)

    val batteryMillivolts: Int? = status.batteryMillivolts

    /** True below [LOW_BATTERY_MILLIVOLTS]; false when the voltage is unknown. */
    val batteryLow: Boolean = batteryMillivolts != null && batteryMillivolts < LOW_BATTERY_MILLIVOLTS

    public companion object {
        /**
         * Two-cell Li-ion pack (7540 mV observed near full). 7000 mV ≈ 3.5 V/cell, the knee of the
         * discharge curve. Provisional until the hardware checklist confirms the warning point.
         */
        public const val LOW_BATTERY_MILLIVOLTS: Int = 7000
    }
}

/** How far a [PrinterClient.print] has got, in transport writes. */
public data class PrintProgress(
    val completedWrites: Int,
    val totalWrites: Int,
) {
    /** 0.0 – 1.0. */
    val fraction: Float get() = if (totalWrites == 0) 0f else completedWrites.toFloat() / totalWrites

    public companion object {
        public val NONE: PrintProgress = PrintProgress(0, 0)
    }
}

/** Why a print did not finish. */
public enum class PrintFailure {
    /** The transport was not connected, or the link dropped mid-job. */
    DISCONNECTED,

    /** `GS r 1` reported paper end before the first raster row was sent. */
    NO_PAPER,

    /** The printer sent `err:` cover-open while the job was in flight. */
    COVER_OPEN,

    /** The printer sent some other `err:` code while the job was in flight. */
    PRINTER_ERROR,

    /** A write was rejected (e.g. too large for the link). */
    WRITE_FAILED,
}

public sealed interface PrintResult {
    public data object Success : PrintResult

    public data class Failure(
        val reason: PrintFailure,
        val progress: PrintProgress,
        val cause: Throwable? = null,
    ) : PrintResult
}

/**
 * Protocol-level operations over a [PrinterTransport]: identify the printer and print a
 * [PrintJob] with the BLE pacing from [com.example.pugprint.printer.PrintTiming]. Operations
 * are serialised by a mutex — the link has no flow control, so only one may talk at a time.
 *
 * @property replyTimeoutMillis how long a query waits for its reply before giving up.
 */
public class PrinterClient(
    private val transport: PrinterTransport,
    private val replyTimeoutMillis: Long = DEFAULT_REPLY_TIMEOUT_MILLIS,
) {
    private val operations = Mutex()

    public val state: StateFlow<TransportState> get() = transport.state

    /** Every notification, decoded. Includes unsolicited `err:` and `LABELOK` events. */
    public val replies: Flow<PrinterReply> = transport.notifications().map(PrinterReply::parse)

    public suspend fun connect(device: PrinterDevice): Unit = transport.connect(device)

    public suspend fun disconnect(): Unit = transport.disconnect()

    /**
     * Queries status, serial and product. The status reply is required; the others fall back
     * to `null` on timeout so an unusual firmware still connects.
     * @throws TransportException when not connected or when the printer does not answer the status query.
     */
    public suspend fun identify(): PrinterIdentity =
        operations.withLock {
            val status =
                query<PrinterReply.Status>(PrinterQueries.queryStatus())
                    ?: throw TransportException("printer did not answer the status query")
            PrinterIdentity(
                status = status,
                serial = query<PrinterReply.Serial>(PrinterQueries.querySerial()),
                product = query<PrinterReply.Product>(PrinterQueries.queryProduct()),
            )
        }

    /**
     * Sends [job] one [com.example.pugprint.printer.PrinterWrite] at a time, sleeping the pause
     * each write demands. Checks the paper sensor first and aborts on cover-open / link loss.
     * Never throws for printer or link problems; those come back as [PrintResult.Failure].
     */
    public suspend fun print(
        job: PrintJob,
        onProgress: (PrintProgress) -> Unit = {},
    ): PrintResult =
        operations.withLock {
            val writes = job.writes()
            var progress = PrintProgress(0, writes.size)
            onProgress(progress)
            if (transport.state.value !is TransportState.Connected) {
                return PrintResult.Failure(PrintFailure.DISCONNECTED, progress)
            }
            if (paperOut()) return PrintResult.Failure(PrintFailure.NO_PAPER, progress)

            coroutineScope {
                var fault: PrinterReply.Error? = null
                val faultWatch =
                    launch(start = CoroutineStart.UNDISPATCHED) {
                        fault =
                            replies
                                .filterIsInstance<PrinterReply.Error>()
                                .first { it.kind != PrinterReply.ErrorKind.CLEARED }
                    }
                try {
                    for (write in writes) {
                        fault?.let { return@coroutineScope failure(it, progress) }
                        if (transport.state.value !is TransportState.Connected) {
                            return@coroutineScope PrintResult.Failure(PrintFailure.DISCONNECTED, progress)
                        }
                        try {
                            transport.write(write.bytes)
                        } catch (e: TransportException) {
                            val reason =
                                if (transport.state.value is TransportState.Connected) {
                                    PrintFailure.WRITE_FAILED
                                } else {
                                    PrintFailure.DISCONNECTED
                                }
                            return@coroutineScope PrintResult.Failure(reason, progress, e)
                        }
                        progress = progress.copy(completedWrites = progress.completedWrites + 1)
                        onProgress(progress)
                        if (write.pauseAfterMillis > 0) delay(write.pauseAfterMillis)
                    }
                    PrintResult.Success
                } finally {
                    faultWatch.cancel()
                }
            }
        }

    private fun failure(
        error: PrinterReply.Error,
        progress: PrintProgress,
    ): PrintResult.Failure =
        PrintResult.Failure(
            if (error.kind ==
                PrinterReply.ErrorKind.COVER_OPEN
            ) {
                PrintFailure.COVER_OPEN
            } else {
                PrintFailure.PRINTER_ERROR
            },
            progress,
        )

    /** `GS r 1` pre-flight; a printer that does not answer is assumed to have paper. */
    private suspend fun paperOut(): Boolean {
        val reply = rawQuery(PrinterQueries.queryPaper()) { it.size == 1 } ?: return false
        return EscPosStatus.paperOutFromGsR(reply.single())
    }

    /** Sends [command] and waits for the first reply that parses as [T]. `null` on timeout. */
    private suspend inline fun <reified T : PrinterReply> query(command: ByteArray): T? =
        rawQuery(command) { PrinterReply.parse(it) is T }?.let { PrinterReply.parse(it) as T }

    private suspend fun rawQuery(
        command: ByteArray,
        accept: (ByteArray) -> Boolean,
    ): ByteArray? {
        if (transport.state.value !is TransportState.Connected) throw TransportException("not connected")
        return try {
            withTimeout(replyTimeoutMillis) {
                // UNDISPATCHED: the collector is subscribed before the query goes out, so the
                // reply cannot slip past it. Transports keep their notification flow hot.
                val reply = async(start = CoroutineStart.UNDISPATCHED) { transport.notifications().firstOrNull(accept) }
                transport.write(command)
                reply.await()
            }
        } catch (_: TimeoutCancellationException) {
            null
        }
    }

    public companion object {
        public const val DEFAULT_REPLY_TIMEOUT_MILLIS: Long = 2_000
    }
}

package com.example.pugprint.bluetooth

import com.example.pugprint.printer.HelloBlinkUuids
import com.example.pugprint.printer.transport.DisconnectCause
import com.example.pugprint.printer.transport.PrinterDevice
import com.example.pugprint.printer.transport.PrinterTransport
import com.example.pugprint.printer.transport.TransportException
import com.example.pugprint.printer.transport.TransportState
import com.juul.kable.Characteristic
import com.juul.kable.Peripheral
import com.juul.kable.State
import com.juul.kable.WriteType
import com.juul.kable.characteristicOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.uuid.Uuid

/**
 * [PrinterTransport] over the Hello Blink's Microchip transparent-UART GATT service, via Kable.
 *
 * - Each [write] is one write-without-response on the RX characteristic. Payloads larger than
 *   the negotiated MTU allows are refused rather than fragmented, because the printer discards
 *   a `GS v 0` block that spans two writes (docs/PRINTER_PROTOCOL.md § BLE constraints).
 * - The TX characteristic is observed for the whole life of a connection and relayed through
 *   [notifications], so a query's reply cannot be missed between subscribing and sending.
 * - An unrequested drop of the link surfaces as [TransportState.Disconnected] with
 *   [DisconnectCause.LOST]; callers decide whether to reconnect.
 *
 * @param peripherals how to obtain a [Peripheral] for a device; [HelloBlinkPeripherals] on hardware.
 * @param scope long-lived scope (application) for the background collectors.
 */
public class BleTransport(
    private val peripherals: PeripheralFactory,
    private val scope: CoroutineScope,
) : PrinterTransport {
    private class Link(
        val device: PrinterDevice,
        val peripheral: Peripheral,
    ) {
        val jobs = ArrayList<Job>()
        var maxWriteBytes: Int = 0
    }

    private val mutableState = MutableStateFlow<TransportState>(TransportState.Disconnected())
    private val replies = MutableSharedFlow<ByteArray>(extraBufferCapacity = REPLY_BUFFER)
    private val lock = Mutex()
    private var link: Link? = null

    override val state: StateFlow<TransportState> = mutableState.asStateFlow()

    override fun notifications(): Flow<ByteArray> = replies

    override suspend fun connect(device: PrinterDevice): Unit =
        lock.withLock {
            closeLink()
            val peripheral = peripherals.create(device)
            val link = Link(device, peripheral)
            this.link = link
            mutableState.value = TransportState.Connecting(device)
            // UNDISPATCHED: subscribed before connecting, so nothing the printer sends is missed.
            link.jobs +=
                scope.launch(start = CoroutineStart.UNDISPATCHED) { peripheral.observe(TX).collect(replies::emit) }
            try {
                peripheral.connect()
                link.maxWriteBytes = peripheral.maximumWriteValueLengthForType(WriteType.WithoutResponse)
            } catch (e: CancellationException) {
                closeLink()
                mutableState.value = TransportState.Disconnected(DisconnectCause.NONE)
                throw e
            } catch (
                // Kable throws the IOException family; Android adds SecurityException / IllegalStateException.
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                closeLink()
                mutableState.value = TransportState.Disconnected(DisconnectCause.FAILED)
                throw TransportException("could not connect to ${device.displayName}", e)
            }
            mutableState.value = TransportState.Connected(device)
            link.jobs +=
                scope.launch(
                    start = CoroutineStart.UNDISPATCHED,
                ) { peripheral.state.collect { onPeripheralState(link, it) } }
        }

    override suspend fun write(bytes: ByteArray) {
        val link = link
        if (link == null || state.value !is TransportState.Connected) throw TransportException("not connected")
        if (bytes.size > link.maxWriteBytes) {
            throw TransportException(
                "write of ${bytes.size} B exceeds the ${link.maxWriteBytes} B link payload; MTU negotiation failed?",
            )
        }
        try {
            link.peripheral.write(RX, bytes, WriteType.WithoutResponse)
        } catch (e: CancellationException) {
            throw e
        } catch (
            // NotConnectedException, GattStatusException, SecurityException…
            @Suppress("TooGenericExceptionCaught") e: Exception,
        ) {
            throw TransportException("write of ${bytes.size} B failed", e)
        }
    }

    override suspend fun disconnect(): Unit =
        lock.withLock {
            closeLink()
            mutableState.value = TransportState.Disconnected(DisconnectCause.NONE)
        }

    /** Mirrors the peripheral's state while [link] is current; a drop we did not ask for is LOST. */
    private fun onPeripheralState(
        link: Link,
        peripheralState: State,
    ) {
        if (this.link !== link) return
        when (peripheralState) {
            is State.Connected -> mutableState.value = TransportState.Connected(link.device)
            State.Disconnecting -> mutableState.value = TransportState.Disconnecting(link.device)
            is State.Disconnected -> {
                closeLink()
                mutableState.value = TransportState.Disconnected(DisconnectCause.LOST)
            }
            is State.Connecting -> Unit
        }
    }

    /** Stops the collectors and releases the peripheral. Never throws. */
    private fun closeLink() {
        val current = link ?: return
        link = null
        current.jobs.forEach(Job::cancel)
        current.peripheral.close()
    }

    private companion object {
        const val REPLY_BUFFER = 64
        val RX: Characteristic = characteristicOf(Uuid.parse(HelloBlinkUuids.SERVICE), Uuid.parse(HelloBlinkUuids.RX))
        val TX: Characteristic = characteristicOf(Uuid.parse(HelloBlinkUuids.SERVICE), Uuid.parse(HelloBlinkUuids.TX))
    }
}

package com.example.pugprint.bluetooth

import app.cash.turbine.test
import com.example.pugprint.printer.HelloBlinkUuids
import com.example.pugprint.printer.transport.DisconnectCause
import com.example.pugprint.printer.transport.PrinterDevice
import com.example.pugprint.printer.transport.TransportException
import com.example.pugprint.printer.transport.TransportState
import com.juul.kable.Characteristic
import com.juul.kable.NotConnectedException
import com.juul.kable.Peripheral
import com.juul.kable.State
import com.juul.kable.WriteType
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.uuid.Uuid

class BleTransportTest {
    private val device = PrinterDevice("AA:BB:CC:DD:EE:FF", "HB-1234")

    /** A scripted Kable peripheral: connect flips [state], writes are recorded, [tx] feeds observe(). */
    private class FakePeripheral(
        scope: CoroutineScope,
        maxWrite: Int = 245,
    ) {
        val state = MutableStateFlow<State>(State.Disconnected())
        val tx = MutableSharedFlow<ByteArray>(extraBufferCapacity = 8)
        val written = ArrayList<Pair<Characteristic, ByteArray>>()
        val observed = slot<Characteristic>()
        val mock: Peripheral =
            mockk {
                every { this@mockk.state } returns this@FakePeripheral.state
                every { observe(capture(observed), any()) } returns tx
                coEvery { connect() } coAnswers {
                    this@FakePeripheral.state.value = State.Connected(scope)
                    scope
                }
                coEvery { maximumWriteValueLengthForType(WriteType.WithoutResponse) } returns maxWrite
                coEvery { write(any<Characteristic>(), any(), any()) } coAnswers {
                    written += firstArg<Characteristic>() to secondArg<ByteArray>()
                }
                coEvery { disconnect() } coAnswers { this@FakePeripheral.state.value = State.Disconnected() }
                every { close() } just Runs
            }
    }

    private fun TestScope.transportFor(peripheral: FakePeripheral) = BleTransport({ peripheral.mock }, backgroundScope)

    @Test
    fun `connect observes TX, connects, and reports Connected`() =
        runTest {
            val peripheral = FakePeripheral(backgroundScope)
            val transport = transportFor(peripheral)

            transport.state.test {
                assertEquals(TransportState.Disconnected(), awaitItem())
                transport.connect(device)
                assertEquals(TransportState.Connecting(device), awaitItem())
                assertEquals(TransportState.Connected(device), awaitItem())
            }
            assertEquals(Uuid.parse(HelloBlinkUuids.SERVICE), peripheral.observed.captured.serviceUuid)
            assertEquals(Uuid.parse(HelloBlinkUuids.TX), peripheral.observed.captured.characteristicUuid)
        }

    @Test
    fun `write goes to RX without response, one payload per write`() =
        runTest {
            val peripheral = FakePeripheral(backgroundScope)
            val transport = transportFor(peripheral)
            transport.connect(device)

            transport.write(byteArrayOf(1, 2, 3))

            val (characteristic, bytes) = peripheral.written.single()
            assertEquals(Uuid.parse(HelloBlinkUuids.RX), characteristic.characteristicUuid)
            assertEquals(listOf<Byte>(1, 2, 3), bytes.toList())
            coVerify(exactly = 1) { peripheral.mock.write(any<Characteristic>(), any(), WriteType.WithoutResponse) }
        }

    @Test
    fun `a payload larger than the MTU allows is refused, never fragmented`() =
        runTest {
            val peripheral = FakePeripheral(backgroundScope, maxWrite = 20)
            val transport = transportFor(peripheral)
            transport.connect(device)

            val thrown = runCatching { transport.write(ByteArray(56)) }.exceptionOrNull()

            assertInstanceOf(TransportException::class.java, thrown)
            assertTrue(peripheral.written.isEmpty())
        }

    @Test
    fun `writing while disconnected throws`() =
        runTest {
            val transport = transportFor(FakePeripheral(backgroundScope))

            val thrown = runCatching { transport.write(byteArrayOf(1)) }.exceptionOrNull()

            assertInstanceOf(TransportException::class.java, thrown)
        }

    @Test
    fun `TX notifications are relayed to notifications()`() =
        runTest {
            val peripheral = FakePeripheral(backgroundScope)
            val transport = transportFor(peripheral)
            transport.connect(device)

            transport.notifications().test {
                peripheral.tx.emit("LABELOK".toByteArray())
                assertEquals("LABELOK", String(awaitItem()))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a dropped link becomes Disconnected LOST and releases the peripheral`() =
        runTest {
            val peripheral = FakePeripheral(backgroundScope)
            val transport = transportFor(peripheral)
            transport.connect(device)

            peripheral.state.value = State.Disconnected(State.Disconnected.Status.PeripheralDisconnected)
            runCurrent()

            assertEquals(TransportState.Disconnected(DisconnectCause.LOST), transport.state.value)
            verify(exactly = 1) { peripheral.mock.close() }
            assertInstanceOf(
                TransportException::class.java,
                runCatching { transport.write(byteArrayOf(1)) }.exceptionOrNull(),
            )
        }

    @Test
    fun `a requested disconnect is NONE, not LOST`() =
        runTest {
            val peripheral = FakePeripheral(backgroundScope)
            val transport = transportFor(peripheral)
            transport.connect(device)

            transport.disconnect()
            peripheral.state.value = State.Disconnected()
            runCurrent()

            assertEquals(TransportState.Disconnected(DisconnectCause.NONE), transport.state.value)
            verify(exactly = 1) { peripheral.mock.close() }
        }

    @Test
    fun `a failed connect is FAILED and wrapped in TransportException`() =
        runTest {
            val peripheral = FakePeripheral(backgroundScope)
            coEvery { peripheral.mock.connect() } throws NotConnectedException()
            val transport = transportFor(peripheral)

            val thrown = runCatching { transport.connect(device) }.exceptionOrNull()

            assertInstanceOf(TransportException::class.java, thrown)
            assertEquals(TransportState.Disconnected(DisconnectCause.FAILED), transport.state.value)
            verify(exactly = 1) { peripheral.mock.close() }
        }

    @Test
    fun `a missing permission surfaces the same way`() =
        runTest {
            val peripheral = FakePeripheral(backgroundScope)
            coEvery { peripheral.mock.connect() } throws SecurityException("Need BLUETOOTH_CONNECT")
            val transport = transportFor(peripheral)

            val thrown = runCatching { transport.connect(device) }.exceptionOrNull()

            assertInstanceOf(TransportException::class.java, thrown)
            assertInstanceOf(SecurityException::class.java, thrown?.cause)
            assertEquals(TransportState.Disconnected(DisconnectCause.FAILED), transport.state.value)
        }

    @Test
    fun `connecting again replaces the previous peripheral`() =
        runTest {
            val first = FakePeripheral(backgroundScope)
            val second = FakePeripheral(backgroundScope)
            val peripherals = ArrayDeque(listOf(first.mock, second.mock))
            val transport = BleTransport({ peripherals.removeFirst() }, backgroundScope)

            transport.connect(device)
            transport.connect(device.copy(id = "11:22:33:44:55:66"))
            transport.write(byteArrayOf(9))

            verify(exactly = 1) { first.mock.close() }
            assertTrue(first.written.isEmpty())
            assertEquals(1, second.written.size)
        }
}

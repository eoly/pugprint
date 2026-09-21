package com.example.pugprint.printer.transport

import app.cash.turbine.test
import com.example.pugprint.printer.Fixtures.hex
import com.example.pugprint.printer.PrinterCommands
import com.example.pugprint.printer.PrinterQueries
import com.example.pugprint.printer.RasterBlock
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FakePrinterTransportTest {
    private val device = PrinterDevice("AA:BB:CC:DD:EE:FF", "HB-1234")

    @Test
    fun `connect walks through connecting to connected`() =
        runTest {
            val transport = FakePrinterTransport()

            transport.state.test {
                assertEquals(TransportState.Disconnected(), awaitItem())
                transport.connect(device)
                assertEquals(TransportState.Connecting(device), awaitItem())
                assertEquals(TransportState.Connected(device), awaitItem())
                transport.disconnect()
                assertEquals(TransportState.Disconnected(DisconnectCause.NONE), awaitItem())
            }
        }

    @Test
    fun `a failing connect reports FAILED and throws`() =
        runTest {
            val transport = FakePrinterTransport().apply { connectFails = true }

            assertFailsWithTransportException { transport.connect(device) }
            assertEquals(TransportState.Disconnected(DisconnectCause.FAILED), transport.state.value)
        }

    @Test
    fun `writes reach the emulator and are recorded`() =
        runTest {
            val transport = FakePrinterTransport()
            transport.connect(device)
            val row = ByteArray(48) { 0x0F }

            transport.write(PrinterCommands.density(25))
            transport.write(RasterBlock.encode(listOf(row)))

            assertEquals(2, transport.writes.size)
            assertEquals(listOf(hex(row)), transport.emulator.rows.map(::hex))
            assertEquals(emptyList<String>(), transport.emulator.violations)
        }

    @Test
    fun `writing while disconnected or oversize throws without touching the emulator`() =
        runTest {
            val transport = FakePrinterTransport(maxWriteBytes = 10)

            assertFailsWithTransportException { transport.write(byteArrayOf(1)) }
            transport.connect(device)
            assertFailsWithTransportException { transport.write(ByteArray(11)) }
            assertTrue(transport.writes.isEmpty())
            assertTrue(transport.emulator.commands.isEmpty())
        }

    @Test
    fun `replies arrive on notifications only for subscribers`() =
        runTest {
            val transport = FakePrinterTransport()
            transport.connect(device)

            transport.write(PrinterQueries.querySerial()) // nobody listening: lost, like a real notification
            transport.notifications().test {
                transport.write(PrinterQueries.queryPaper())
                assertEquals("00", hex(awaitItem()))
                transport.notify("err:\u0002.".toByteArray())
                assertEquals("err:\u0002.", String(awaitItem()))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `dropAfterWrites loses the link and later writes fail`() =
        runTest {
            val transport = FakePrinterTransport().apply { dropAfterWrites = 1 }
            transport.connect(device)

            transport.write(PrinterCommands.init())

            assertEquals(TransportState.Disconnected(DisconnectCause.LOST), transport.state.value)
            assertFailsWithTransportException { transport.write(PrinterCommands.init()) }
        }

    private inline fun assertFailsWithTransportException(block: () -> Unit) {
        val thrown = runCatching(block).exceptionOrNull()
        assertInstanceOf(TransportException::class.java, thrown)
    }
}

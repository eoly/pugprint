package com.example.pugprint.printer

import com.example.pugprint.imaging.MonoBitmap
import com.example.pugprint.imaging.TestPattern
import com.example.pugprint.printer.transport.FakePrinterTransport
import com.example.pugprint.printer.transport.PrintFailure
import com.example.pugprint.printer.transport.PrintResult
import com.example.pugprint.printer.transport.PrinterClient
import com.example.pugprint.printer.transport.PrinterDevice
import com.example.pugprint.printer.transport.TransportState
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class PrinterManagerTest {
    private val device = PrinterDevice("AA:BB:CC:DD:EE:FF", "HB-1234")
    private val transport = FakePrinterTransport()
    private val store = InMemoryPairedPrinterStore()
    private var permissionGranted = true
    private val backoffCalls = ArrayList<Int>()

    private fun TestScope.manager() =
        PrinterManager(
            client = PrinterClient(transport),
            store = store,
            permission = { permissionGranted },
            scope = backgroundScope,
            backoff = { attempt ->
                backoffCalls += attempt
                1_000L
            },
        )

    /** `err:<code>.` as the printer sends it. */
    private fun errNotification(code: Int): ByteArray =
        "err:".toByteArray() + byteArrayOf(code.toByte()) + ".".toByteArray()

    @Test
    fun `starts with no printer when nothing is paired`() =
        runTest {
            val manager = manager()
            manager.start()
            runCurrent()

            assertEquals(PrinterState.NoPrinter, manager.state.value)
            assertEquals(TransportState.Disconnected(), transport.state.value)
        }

    @Test
    fun `connect remembers the device, connects and identifies`() =
        runTest {
            val manager = manager()

            manager.connect(device)
            runCurrent()

            val connected = assertInstanceOf(PrinterState.Connected::class.java, manager.state.value)
            assertEquals(device, connected.device)
            assertEquals(7540, connected.identity.batteryMillivolts)
            assertEquals(device, store.load())
        }

    @Test
    fun `start reconnects to the remembered printer`() =
        runTest {
            store.save(device)
            val manager = manager()

            manager.start()
            runCurrent()

            assertInstanceOf(PrinterState.Connected::class.java, manager.state.value)
        }

    @Test
    fun `start holds off without the Bluetooth permission and retry proceeds once granted`() =
        runTest {
            store.save(device)
            permissionGranted = false
            val manager = manager()

            manager.start()
            runCurrent()
            assertEquals(PrinterState.Offline(device, OfflineReason.NO_PERMISSION), manager.state.value)

            permissionGranted = true
            manager.retry()
            runCurrent()
            assertInstanceOf(PrinterState.Connected::class.java, manager.state.value)
        }

    @Test
    fun `an unreachable printer is retried with backoff`() =
        runTest {
            transport.connectFails = true
            val manager = manager()

            manager.connect(device)
            runCurrent()
            assertEquals(PrinterState.Offline(device, OfflineReason.UNREACHABLE), manager.state.value)
            assertEquals(listOf(1), backoffCalls)

            advanceTimeBy(1_001)
            assertEquals(listOf(1, 2), backoffCalls)

            transport.connectFails = false
            advanceTimeBy(1_001)
            assertInstanceOf(PrinterState.Connected::class.java, manager.state.value)
        }

    @Test
    fun `a lost link goes Offline LOST and reconnects`() =
        runTest {
            val manager = manager()
            manager.connect(device)
            runCurrent()

            transport.dropConnection()
            runCurrent()
            assertEquals(PrinterState.Offline(device, OfflineReason.LOST), manager.state.value)

            advanceTimeBy(1_001)
            val connected = assertInstanceOf(PrinterState.Connected::class.java, manager.state.value)
            assertEquals(device, connected.device)
        }

    @Test
    fun `printing the test page sends every row and reports success`() =
        runTest {
            val manager = manager()
            manager.connect(device)
            runCurrent()

            manager.printTestPage()
            runCurrent()
            assertInstanceOf(PrinterState.Printing::class.java, manager.state.value)
            advanceTimeBy(60_000)

            assertInstanceOf(PrinterState.Connected::class.java, manager.state.value)
            assertEquals(PrintResult.Success, manager.lastPrintResult.value)
            assertEquals(TestPattern.render().height, transport.emulator.rows.size)
            assertEquals(25, transport.emulator.density) // public-factory medium
            assertEquals(emptyList<String>(), transport.emulator.violations)

            manager.dismissPrintResult()
            assertNull(manager.lastPrintResult.value)
        }

    @Test
    fun `printing an image sends exactly its rows`() =
        runTest {
            val manager = manager()
            manager.connect(device)
            runCurrent()
            val image = MonoBitmap.fromPixels(384, 3, BooleanArray(384 * 3) { it % 384 < 8 })

            manager.printImage(image)
            advanceTimeBy(5_000)

            assertEquals(PrintResult.Success, manager.lastPrintResult.value)
            assertEquals(3, transport.emulator.rows.size)
            assertEquals(
                0xFF.toByte(),
                transport.emulator.rows
                    .first()
                    .first(),
            )
            assertEquals(emptyList<String>(), transport.emulator.violations)
        }

    @Test
    fun `an image that is not head-width is refused up front`() =
        runTest {
            val manager = manager()
            val narrow = MonoBitmap.fromPixels(8, 1, BooleanArray(8))
            assertThrows(IllegalArgumentException::class.java) { manager.printImage(narrow) }
        }

    @Test
    fun `an empty paper bay fails the print and leaves the printer connected`() =
        runTest {
            transport.emulator.paperPresent = false
            val manager = manager()
            manager.connect(device)
            runCurrent()

            manager.printTestPage()
            advanceTimeBy(60_000)

            val failure = assertInstanceOf(PrintResult.Failure::class.java, manager.lastPrintResult.value)
            assertEquals(PrintFailure.NO_PAPER, failure.reason)
            assertInstanceOf(PrinterState.Connected::class.java, manager.state.value)
            assertTrue(transport.emulator.rows.isEmpty())
        }

    @Test
    fun `a lid or paper notification is reflected and cleared`() =
        runTest {
            val manager = manager()
            manager.connect(device)
            runCurrent()

            transport.notify(errNotification(PrinterReply.ERROR_LID_OR_PAPER))
            runCurrent()
            assertTrue((manager.state.value as PrinterState.Connected).paperOrLidProblem)

            transport.notify(errNotification(PrinterReply.ERROR_CLEARED))
            runCurrent()
            assertFalse((manager.state.value as PrinterState.Connected).paperOrLidProblem)
        }

    @Test
    fun `forget disconnects, clears the store and stops reconnecting`() =
        runTest {
            val manager = manager()
            manager.connect(device)
            runCurrent()

            manager.forget()
            runCurrent()

            assertEquals(PrinterState.NoPrinter, manager.state.value)
            assertNull(store.load())
            assertEquals(TransportState.Disconnected(), transport.state.value)
            advanceTimeBy(60_000)
            assertEquals(PrinterState.NoPrinter, manager.state.value)
        }

    @Test
    fun `exponential backoff doubles from one second and caps at thirty`() {
        val backoff = ReconnectBackoff.EXPONENTIAL
        assertEquals(
            listOf(1_000L, 2_000L, 4_000L, 8_000L, 16_000L, 30_000L, 30_000L),
            (1..7).map(backoff::delayMillis),
        )
    }

    @ParameterizedTest
    @EnumSource(DensityLevel::class)
    fun `prints at the requested density from this printer's table`(level: DensityLevel) =
        runTest {
            val manager = manager()
            manager.connect(device)
            runCurrent()

            manager.printImage(TestPattern.render(), level)
            advanceTimeBy(60_000)

            assertEquals(PrintResult.Success, manager.lastPrintResult.value)
            assertEquals(DensityProfile.PUBLIC.value(level), transport.emulator.density)
        }

    @Test
    fun `print again repeats the last sticker at the same darkness`() =
        runTest {
            val manager = manager()
            manager.connect(device)
            runCurrent()
            assertNull(manager.lastPrint.value)
            manager.printAgain() // nothing to repeat yet: no-op
            runCurrent()
            assertInstanceOf(PrinterState.Connected::class.java, manager.state.value)

            val sticker = MonoBitmap.fromPixels(384, 3, BooleanArray(384 * 3) { it % 2 == 0 })
            manager.printImage(sticker, DensityLevel.DARK)
            advanceTimeBy(60_000)
            assertEquals(LastPrint(sticker, DensityLevel.DARK), manager.lastPrint.value)
            assertEquals(3, transport.emulator.rows.size)

            manager.printAgain()
            advanceTimeBy(60_000)

            assertEquals(PrintResult.Success, manager.lastPrintResult.value)
            assertEquals(6, transport.emulator.rows.size)
            assertEquals(DensityProfile.PUBLIC.value(DensityLevel.DARK), transport.emulator.density)
        }
}

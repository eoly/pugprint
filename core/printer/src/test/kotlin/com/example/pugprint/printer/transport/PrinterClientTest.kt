package com.example.pugprint.printer.transport

import com.example.pugprint.printer.CommandDecoder
import com.example.pugprint.printer.DensityProfile
import com.example.pugprint.printer.Fixtures
import com.example.pugprint.printer.Fixtures.hex
import com.example.pugprint.printer.PrintJob
import com.example.pugprint.printer.PrintTiming
import com.example.pugprint.printer.PrinterCommand
import com.example.pugprint.printer.PrinterEmulator
import com.example.pugprint.printer.PrinterReply
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PrinterClientTest {
    private val device = PrinterDevice("AA:BB:CC:DD:EE:FF", "HB-1234")
    private val rows = Fixtures.pbmRows("pug_arrow")

    @Test
    fun `identify reads status, serial and product from the printer`() =
        runTest {
            val transport = FakePrinterTransport()
            val client = PrinterClient(transport)
            client.connect(device)

            val identity = client.identify()

            assertEquals(PrinterReply.Status("H1.0", "V1.01", 7540, 384), identity.status)
            assertEquals(PrinterReply.Serial("HBHW250000001234"), identity.serial)
            assertEquals(PrinterReply.Product(801), identity.product)
            assertEquals(DensityProfile.PUBLIC, identity.densityProfile)
            assertEquals(7540, identity.batteryMillivolts)
            assertFalse(identity.batteryLow)
            assertEquals(
                listOf(PrinterCommand.Query.STATUS, PrinterCommand.Query.SERIAL, PrinterCommand.Query.PRODUCT),
                transport.emulator.commands,
            )
        }

    @Test
    fun `identify flags a low battery and picks the private profile from the product id`() =
        runTest {
            val identityOnWire =
                PrinterEmulator.Identity(
                    batteryMillivolts = 6900,
                    firmwareVersion = "V1.10",
                    productId = 12,
                )
            val client = PrinterClient(FakePrinterTransport(PrinterEmulator(identityOnWire)))
            client.connect(device)

            val identity = client.identify()

            assertTrue(identity.batteryLow)
            assertEquals(DensityProfile.PRIVATE_OLD, identity.densityProfile)
        }

    @Test
    fun `identify fails fast when disconnected`() =
        runTest {
            val client = PrinterClient(FakePrinterTransport())

            assertFailsWithTransportException { client.identify() }
        }

    @Test
    fun `identify times out per query and throws only when status is missing`() =
        runTest {
            val transport = MuteTransport()
            val client = PrinterClient(transport, replyTimeoutMillis = 100)
            client.connect(device)

            assertFailsWithTransportException { client.identify() }
        }

    @Test
    fun `print sends the whole job, paced, and the emulator prints every row`() =
        runTest {
            val transport = FakePrinterTransport()
            val client = PrinterClient(transport)
            client.connect(device)
            val progress = ArrayList<PrintProgress>()
            val start = testScheduler.currentTime

            val result = client.print(PrintJob(rows, density = 25), progress::add)

            assertEquals(PrintResult.Success, result)
            assertEquals(emptyList<String>(), transport.emulator.violations)
            assertEquals(rows.map(::hex), transport.emulator.rows.map(::hex))
            val expectedWrites = 3 + rows.size // density, init, feed + one block per row
            assertEquals(1 + expectedWrites, transport.writes.size) // + the paper pre-flight query
            assertEquals(PrintProgress(0, expectedWrites), progress.first())
            assertEquals(PrintProgress(expectedWrites, expectedWrites), progress.last())
            assertEquals(1f, progress.last().fraction)
            val expectedMillis = (rows.size - 1) * PrintTiming.BLOCK_GAP_MILLIS + PrintTiming.BEFORE_FEED_MILLIS
            assertEquals(expectedMillis, testScheduler.currentTime - start)
        }

    @Test
    fun `every write is one command group and no raster block exceeds a BLE payload`() =
        runTest {
            val transport = FakePrinterTransport()
            val client = PrinterClient(transport)
            client.connect(device)

            client.print(PrintJob(rows, density = 25))

            transport.writes.forEach { write ->
                assertTrue(write.size <= FakePrinterTransport.DEFAULT_MAX_WRITE_BYTES)
                val rasters = CommandDecoder.decode(write).filterIsInstance<PrinterCommand.Raster>()
                assertTrue(rasters.size <= 1, "at most one raster block per write")
                rasters.forEach { assertEquals(1, it.rows.size, "one row per block") }
            }
        }

    @Test
    fun `print refuses when not connected`() =
        runTest {
            val client = PrinterClient(FakePrinterTransport())

            val result = client.print(PrintJob(rows, density = 25))

            assertEquals(PrintResult.Failure(PrintFailure.DISCONNECTED, PrintProgress(0, rows.size + 3)), result)
        }

    @Test
    fun `print aborts before the raster when the paper bay is empty`() =
        runTest {
            val transport = FakePrinterTransport().apply { emulator.paperPresent = false }
            val client = PrinterClient(transport)
            client.connect(device)

            val result = client.print(PrintJob(rows, density = 25))

            assertInstanceOf(PrintResult.Failure::class.java, result)
            assertEquals(PrintFailure.NO_PAPER, (result as PrintResult.Failure).reason)
            assertNull(transport.emulator.density)
            assertTrue(transport.emulator.rows.isEmpty())
        }

    @Test
    fun `print reports DISCONNECTED with partial progress when the link drops`() =
        runTest {
            // Accept the paper query, density, init and 10 raster blocks, then lose the link.
            val transport = FakePrinterTransport().apply { dropAfterWrites = 1 + 2 + 10 }
            val client = PrinterClient(transport)
            client.connect(device)

            val result = client.print(PrintJob(rows, density = 25))

            assertInstanceOf(PrintResult.Failure::class.java, result)
            result as PrintResult.Failure
            assertEquals(PrintFailure.DISCONNECTED, result.reason)
            assertEquals(12, result.progress.completedWrites)
            assertEquals(10, transport.emulator.rows.size)
        }

    @Test
    fun `an err code 2 notification mid-job aborts with LID_OR_PAPER`() =
        runTest {
            val transport = FakePrinterTransport()
            val client = PrinterClient(transport)
            client.connect(device)
            var result: PrintResult? = null
            val job = launch { result = client.print(PrintJob(rows, density = 25)) }

            advanceTimeBy(5 * PrintTiming.BLOCK_GAP_MILLIS + 1)
            transport.notify("err:\u0002.".toByteArray())
            advanceUntilIdle()

            job.join()
            assertInstanceOf(PrintResult.Failure::class.java, result)
            assertEquals(PrintFailure.LID_OR_PAPER, (result as PrintResult.Failure).reason)
            assertTrue(transport.emulator.rows.size < rows.size)
        }

    @Test
    fun `err cleared does not abort but another code does`() =
        runTest {
            val transport = FakePrinterTransport()
            val client = PrinterClient(transport)
            client.connect(device)
            var result: PrintResult? = null
            val job = launch { result = client.print(PrintJob(rows, density = 25)) }

            advanceTimeBy(PrintTiming.BLOCK_GAP_MILLIS + 1)
            transport.notify("err:\u0000.".toByteArray())
            advanceTimeBy(PrintTiming.BLOCK_GAP_MILLIS + 1)
            transport.notify("err:\u0004.".toByteArray())
            advanceUntilIdle()

            job.join()
            assertEquals(PrintFailure.PRINTER_ERROR, (result as PrintResult.Failure).reason)
        }

    @Test
    fun `operations are serialised so a second print waits for the first`() =
        runTest {
            val transport = FakePrinterTransport()
            val client = PrinterClient(transport)
            client.connect(device)
            val first = launch { client.print(PrintJob(rows.take(4), density = 25)) }
            val second = launch { client.print(PrintJob(rows.take(4), density = 25)) }

            advanceUntilIdle()
            first.join()
            second.join()

            val densities = transport.emulator.commands.filterIsInstance<PrinterCommand.Density>()
            assertEquals(2, densities.size)
            assertEquals(8, transport.emulator.rows.size)
            assertEquals(emptyList<String>(), transport.emulator.violations)
        }

    @Test
    fun `replies flow decodes unsolicited notifications`() =
        runTest {
            val transport = FakePrinterTransport()
            val client = PrinterClient(transport)
            val seen = ArrayList<PrinterReply>()
            val collector = launch { client.replies.collect(seen::add) }
            advanceUntilIdle()

            transport.notify("LABELOK".toByteArray())
            transport.notify("err:\u0002.".toByteArray())
            advanceUntilIdle()
            collector.cancel()

            assertEquals(listOf(PrinterReply.LabelOk, PrinterReply.Error(2)), seen)
        }

    /** A printer that accepts writes but never answers anything. */
    private class MuteTransport : PrinterTransport {
        private val inner = FakePrinterTransport()
        override val state get() = inner.state

        override fun notifications(): Flow<ByteArray> = flow { awaitCancellation() }

        override suspend fun connect(device: PrinterDevice) = inner.connect(device)

        override suspend fun write(bytes: ByteArray) = inner.write(bytes)

        override suspend fun disconnect() = inner.disconnect()
    }

    private inline fun assertFailsWithTransportException(block: () -> Unit) {
        val thrown = runCatching(block).exceptionOrNull()
        assertInstanceOf(TransportException::class.java, thrown)
    }
}

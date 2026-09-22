package com.example.pugprint.ui.home

import android.content.Intent
import com.example.pugprint.design.theme.ThemeCatalog
import com.example.pugprint.printer.DensityLevel
import com.example.pugprint.printer.InMemoryPairedPrinterStore
import com.example.pugprint.printer.PairingStart
import com.example.pugprint.printer.PrinterManager
import com.example.pugprint.printer.PrinterPairing
import com.example.pugprint.printer.transport.FakePrinterTransport
import com.example.pugprint.printer.transport.PrinterClient
import com.example.pugprint.printer.transport.PrinterDevice
import com.example.pugprint.settings.AppSettings
import com.example.pugprint.settings.InMemorySettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val device = PrinterDevice("AA:BB:CC:DD:EE:FF", "HB-1234")
    private val transport = FakePrinterTransport()
    private val store = InMemoryPairedPrinterStore()
    private val settings = InMemorySettingsStore()

    /** Scripted pairing: what `begin()` returns and what the picker result decodes to. */
    private class ScriptedPairing(
        var start: PairingStart,
        var decoded: PrinterDevice? = null,
    ) : PrinterPairing {
        override suspend fun begin(): PairingStart = start

        override fun deviceFrom(result: Intent?): PrinterDevice? = decoded
    }

    @BeforeEach
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(pairing: PrinterPairing) =
        HomeViewModel(
            printer = PrinterManager(PrinterClient(transport), store, { true }, backgroundScope) { 1_000L },
            pairing = pairing,
            settings = settings,
        )

    @Test
    fun `starts with no printer and printing disabled`() =
        runTest {
            val viewModel = viewModel(ScriptedPairing(PairingStart.Paired(device)))
            runCurrent()

            val state = viewModel.uiState.value
            assertEquals(PrinterStatus.NoPrinter, state.printerStatus)
            assertFalse(state.canPrint)
            assertFalse(state.hasPrinter)
        }

    @Test
    fun `pairing without a picker connects straight away`() =
        runTest {
            val viewModel = viewModel(ScriptedPairing(PairingStart.Paired(device)))

            viewModel.onConnectClicked()
            runCurrent()

            val state = viewModel.uiState.value
            assertEquals(PrinterStatus.Connected, state.printerStatus)
            assertEquals("HB-1234", state.printerName)
            assertEquals(HomeViewModel.batteryPercent(7540), state.batteryPercent)
            assertTrue(state.canPrint)
            assertNull(viewModel.pairingLaunch.value)
        }

    @Test
    fun `a picker result connects to the chosen printer, a cancelled one shows a message`() =
        runTest {
            val pairing = ScriptedPairing(PairingStart.Unavailable("n/a"), decoded = null)
            val viewModel = viewModel(pairing)

            viewModel.onPairingResult(null)
            runCurrent()
            assertEquals(HomeMessage.PairingCancelled, viewModel.uiState.value.message)
            viewModel.onMessageShown()
            assertNull(viewModel.uiState.value.message)

            pairing.decoded = device
            viewModel.onPairingResult(Intent())
            runCurrent()
            assertEquals(PrinterStatus.Connected, viewModel.uiState.value.printerStatus)
        }

    @Test
    fun `unavailable pairing surfaces a message and re-enables the button`() =
        runTest {
            val viewModel = viewModel(ScriptedPairing(PairingStart.Unavailable("no CDM")))

            viewModel.onConnectClicked()
            runCurrent()

            assertEquals(HomeMessage.PairingUnavailable, viewModel.uiState.value.message)
            assertFalse(viewModel.uiState.value.pairingInProgress)
        }

    @Test
    fun `printing shows progress then a done message`() =
        runTest {
            val viewModel = viewModel(ScriptedPairing(PairingStart.Paired(device)))
            viewModel.onConnectClicked()
            runCurrent()

            viewModel.onPrintTestPageClicked()
            runCurrent()
            assertEquals(PrinterStatus.Printing, viewModel.uiState.value.printerStatus)
            assertFalse(viewModel.uiState.value.canPrint)
            advanceTimeBy(60_000)

            assertEquals(PrinterStatus.Connected, viewModel.uiState.value.printerStatus)
            assertEquals(HomeMessage.PrintDone, viewModel.uiState.value.message)
            viewModel.onMessageShown()
            assertNull(viewModel.uiState.value.message)
        }

    @Test
    fun `losing the printer shows Offline and forget returns to no printer`() =
        runTest {
            val viewModel = viewModel(ScriptedPairing(PairingStart.Paired(device)))
            viewModel.onConnectClicked()
            runCurrent()

            transport.dropConnection()
            runCurrent()
            assertEquals(PrinterStatus.Offline, viewModel.uiState.value.printerStatus)

            viewModel.onForgetClicked()
            runCurrent()
            assertEquals(PrinterStatus.NoPrinter, viewModel.uiState.value.printerStatus)
        }

    @Test
    fun `battery percent is a clamped linear gauge`() {
        assertEquals(0, HomeViewModel.batteryPercent(5_000))
        assertEquals(50, HomeViewModel.batteryPercent(7_200))
        assertEquals(100, HomeViewModel.batteryPercent(9_000))
    }

    @Test
    fun `picking a look is remembered and shown`() =
        runTest {
            val viewModel = viewModel(ScriptedPairing(PairingStart.Paired(device)))
            runCurrent()
            assertEquals(ThemeCatalog.default.id, viewModel.uiState.value.themeId)

            viewModel.onThemeSelected("ocean")
            runCurrent()

            assertEquals("ocean", viewModel.uiState.value.themeId)
            assertEquals("ocean", settings.settings.value.themeId)
        }

    @Test
    fun `an unknown look falls back to the default`() =
        runTest {
            val viewModel = viewModel(ScriptedPairing(PairingStart.Paired(device)))
            viewModel.onThemeSelected("no-such-theme")
            runCurrent()

            assertEquals(ThemeCatalog.default.id, viewModel.uiState.value.themeId)
        }

    @Test
    fun `the test page prints at the chosen darkness`() =
        runTest {
            settings.update { it.copy(density = DensityLevel.DARK) }
            val viewModel = viewModel(ScriptedPairing(PairingStart.Paired(device)))
            viewModel.onConnectClicked()
            runCurrent()

            viewModel.onPrintTestPageClicked()
            advanceTimeBy(60_000)

            assertEquals(30, transport.emulator.density) // public-factory dark
            assertEquals(AppSettings(density = DensityLevel.DARK), settings.settings.value)
        }

    @Test
    fun `print again appears after the first print and repeats it`() =
        runTest {
            val viewModel = viewModel(ScriptedPairing(PairingStart.Paired(device)))
            viewModel.onConnectClicked()
            runCurrent()
            assertFalse(viewModel.uiState.value.hasLastPrint)
            assertFalse(viewModel.uiState.value.canPrintAgain)

            viewModel.onPrintTestPageClicked()
            advanceTimeBy(60_000)
            assertTrue(viewModel.uiState.value.hasLastPrint)
            assertTrue(viewModel.uiState.value.canPrintAgain)
            val rowsAfterFirst = transport.emulator.rows.size

            viewModel.onPrintAgainClicked()
            runCurrent()
            assertEquals(PrinterStatus.Printing, viewModel.uiState.value.printerStatus)
            advanceTimeBy(60_000)

            assertEquals(rowsAfterFirst * 2, transport.emulator.rows.size)
            assertEquals(HomeMessage.PrintDone, viewModel.uiState.value.message)
        }
}

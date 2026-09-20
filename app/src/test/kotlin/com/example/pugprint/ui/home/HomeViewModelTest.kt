package com.example.pugprint.ui.home

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class HomeViewModelTest {
    @Test
    fun `starts with no printer and printing disabled`() =
        runTest {
            HomeViewModel().uiState.test {
                val state = awaitItem()
                assertEquals(PrinterStatus.NotConnected, state.printerStatus)
                assertFalse(state.canPrint)
            }
        }
}

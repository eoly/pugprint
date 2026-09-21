package com.example.pugprint.printer

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.example.pugprint.bluetooth.BluetoothPermissions
import com.example.pugprint.bluetooth.CompanionPairing
import com.example.pugprint.bluetooth.PairingException
import com.example.pugprint.printer.transport.PrinterDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** First step of pairing, as decided by the platform. */
sealed interface PairingStart {
    /** Show the system picker: launch [intentSender] for a result and hand it to [PrinterPairing.deviceFrom]. */
    data class Launch(
        val intentSender: IntentSender,
    ) : PairingStart

    /** No UI needed (fake printer). */
    data class Paired(
        val device: PrinterDevice,
    ) : PairingStart

    data class Unavailable(
        val reason: String,
    ) : PairingStart
}

/** How the app finds a printer; the real one is the Companion Device Manager. */
interface PrinterPairing {
    suspend fun begin(): PairingStart

    fun deviceFrom(result: Intent?): PrinterDevice?
}

/** Whether the app may connect right now (runtime Bluetooth permission on Android 12+). */
fun interface ConnectPermission {
    fun isGranted(): Boolean
}

class CompanionPrinterPairing
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : PrinterPairing {
        private val companion = CompanionPairing(context)

        override suspend fun begin(): PairingStart {
            if (!companion.isSupported()) return PairingStart.Unavailable("This device cannot pair companion devices")
            return try {
                PairingStart.Launch(companion.requestAssociation())
            } catch (e: PairingException) {
                PairingStart.Unavailable(e.message ?: "pairing failed")
            }
        }

        override fun deviceFrom(result: Intent?): PrinterDevice? = companion.deviceFromResult(result)
    }

class BluetoothConnectPermission
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : ConnectPermission {
        override fun isGranted(): Boolean = BluetoothPermissions.missing(context).isEmpty()
    }

/** Pairs instantly with the emulator-backed printer of the fake-printer build. */
class FakePrinterPairing : PrinterPairing {
    override suspend fun begin(): PairingStart = PairingStart.Paired(FAKE_DEVICE)

    override fun deviceFrom(result: Intent?): PrinterDevice? = FAKE_DEVICE

    companion object {
        val FAKE_DEVICE = PrinterDevice("00:11:22:33:44:55", "HB-FAKE")
    }
}

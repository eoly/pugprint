package com.example.pugprint.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothLeDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.os.Parcelable
import androidx.annotation.RequiresApi
import androidx.core.content.IntentCompat
import com.example.pugprint.printer.HelloBlinkUuids
import com.example.pugprint.printer.transport.PrinterDevice
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** The system could not run the Companion Device Manager association flow. */
public class PairingException(
    message: String,
) : Exception(message)

/**
 * Pairing through the Companion Device Manager (CDM). The system scans on the app's behalf
 * for devices advertising the Hello Blink UART service and shows the picker, so the app needs
 * neither `BLUETOOTH_SCAN` nor any location permission (ADR 0006).
 *
 * Flow: [requestAssociation] → launch the returned [IntentSender] for a result →
 * [deviceFromResult] on the result's data.
 */
public class CompanionPairing(
    private val context: Context,
) {
    private val manager: CompanionDeviceManager?
        get() = context.getSystemService(CompanionDeviceManager::class.java)

    /** False on devices without the companion-device feature (some tablets, emulators). */
    public fun isSupported(): Boolean =
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_COMPANION_DEVICE_SETUP) && manager != null

    /** BLE devices advertising [HelloBlinkUuids.SERVICE]; the user picks one from the system list. */
    public fun associationRequest(): AssociationRequest {
        val scanFilter = ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(HelloBlinkUuids.SERVICE)).build()
        val deviceFilter = BluetoothLeDeviceFilter.Builder().setScanFilter(scanFilter).build()
        return AssociationRequest
            .Builder()
            .addDeviceFilter(deviceFilter)
            .setSingleDevice(false)
            .build()
    }

    /**
     * Starts the association and suspends until the system has a picker to show. Launch the
     * result with `ActivityResultContracts.StartIntentSenderForResult`.
     * @throws PairingException when CDM is unavailable or reports a failure.
     */
    public suspend fun requestAssociation(): IntentSender {
        val manager = manager ?: throw PairingException("Companion Device Manager is not available")
        return suspendCancellableCoroutine { continuation ->
            val callback =
                object : CompanionDeviceManager.Callback() {
                    override fun onAssociationPending(intentSender: IntentSender) {
                        if (continuation.isActive) continuation.resume(intentSender)
                    }

                    @Deprecated("Called on API < 33; API 33+ uses onAssociationPending.")
                    override fun onDeviceFound(intentSender: IntentSender) {
                        if (continuation.isActive) continuation.resume(intentSender)
                    }

                    override fun onAssociationCreated(associationInfo: AssociationInfo) = Unit

                    override fun onFailure(error: CharSequence?) {
                        if (continuation.isActive) {
                            continuation.resumeWithException(PairingException(error?.toString() ?: "pairing failed"))
                        }
                    }
                }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                manager.associate(associationRequest(), context.mainExecutor, callback)
            } else {
                @Suppress("DEPRECATION")
                manager.associate(associationRequest(), callback, null)
            }
        }
    }

    /** Decodes the picker's result. `null` when the user cancelled or the payload is not a Bluetooth device. */
    public fun deviceFromResult(data: Intent?): PrinterDevice? {
        if (data == null) return null
        val extra = IntentCompat.getParcelableExtra(data, CompanionDeviceManager.EXTRA_DEVICE, Parcelable::class.java)
        return when (extra) {
            is ScanResult -> PrinterDevice(extra.device.address.uppercase(), extra.scanRecord?.deviceName)
            is BluetoothDevice -> PrinterDevice(extra.address.uppercase(), nameOf(extra))
            else -> associationDevice(data)
        }
    }

    private fun associationDevice(data: Intent): PrinterDevice? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) associationDeviceApi33(data) else null

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun associationDeviceApi33(data: Intent): PrinterDevice? {
        val info =
            IntentCompat.getParcelableExtra(data, CompanionDeviceManager.EXTRA_ASSOCIATION, AssociationInfo::class.java)
        val address = info?.deviceMacAddress?.toString()?.uppercase()
        return address?.let { PrinterDevice(it, info.displayName?.toString()) }
    }

    /** The cached name needs `BLUETOOTH_CONNECT` on API 31+; without it we show the address instead. */
    @SuppressLint("MissingPermission")
    private fun nameOf(device: BluetoothDevice): String? =
        try {
            device.name
        } catch (_: SecurityException) {
            null
        }
}

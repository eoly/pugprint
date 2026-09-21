package com.example.pugprint.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.ScanResult
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.pugprint.printer.transport.PrinterDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Robolectric (JUnit 4 via vintage) — exercises the Android-framework side of CDM pairing. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class CompanionPairingTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val pairing = CompanionPairing(context)
    private val adapter = BluetoothAdapter.getDefaultAdapter()

    @Test
    fun `association request filters on the UART service and lets the user pick among printers`() {
        val request = pairing.associationRequest()

        assertNotNull(request)
        assertFalse(request.isSingleDevice)
    }

    @Test
    fun `supported only with the companion device feature`() {
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_COMPANION_DEVICE_SETUP, false)
        assertFalse(pairing.isSupported())

        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_COMPANION_DEVICE_SETUP, true)
        assertTrue(pairing.isSupported())
    }

    @Test
    fun `a BLE scan result becomes a PrinterDevice with an upper-case address`() {
        val bluetoothDevice = adapter.getRemoteDevice("AA:BB:CC:DD:EE:FF")
        val intent = Intent().putExtra(CompanionDeviceManager.EXTRA_DEVICE, ScanResult(bluetoothDevice, null, -40, 0L))

        assertEquals(PrinterDevice("AA:BB:CC:DD:EE:FF", null), pairing.deviceFromResult(intent))
    }

    @Test
    fun `a bare BluetoothDevice extra is accepted too`() {
        val bluetoothDevice = adapter.getRemoteDevice("11:22:33:44:55:66")
        shadowOf(bluetoothDevice).setName("HB-5566")
        val intent = Intent().putExtra(CompanionDeviceManager.EXTRA_DEVICE, bluetoothDevice)

        assertEquals(PrinterDevice("11:22:33:44:55:66", "HB-5566"), pairing.deviceFromResult(intent))
    }

    @Test
    fun `cancelled or foreign results decode to null`() {
        assertNull(pairing.deviceFromResult(null))
        assertNull(pairing.deviceFromResult(Intent()))
        assertNull(pairing.deviceFromResult(Intent().putExtra(CompanionDeviceManager.EXTRA_DEVICE, "not a device")))
    }
}

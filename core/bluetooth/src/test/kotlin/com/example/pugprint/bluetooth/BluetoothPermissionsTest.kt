package com.example.pugprint.bluetooth

import android.Manifest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BluetoothPermissionsTest {
    @Test
    fun `Android 12 and later need BLUETOOTH_CONNECT only`() {
        assertEquals(listOf(Manifest.permission.BLUETOOTH_CONNECT), BluetoothPermissions.required(sdkInt = 31))
        assertEquals(listOf(Manifest.permission.BLUETOOTH_CONNECT), BluetoothPermissions.required(sdkInt = 37))
    }

    @Test
    fun `before Android 12 nothing is requested at runtime`() {
        assertEquals(emptyList<String>(), BluetoothPermissions.required(sdkInt = 26))
        assertEquals(emptyList<String>(), BluetoothPermissions.required(sdkInt = 30))
    }
}

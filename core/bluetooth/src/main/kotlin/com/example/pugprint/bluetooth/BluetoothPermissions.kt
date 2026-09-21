package com.example.pugprint.bluetooth

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Runtime permissions PugPrint needs for Bluetooth (ADR 0006). Pairing goes through the
 * Companion Device Manager, so connecting is the only Bluetooth operation the app performs
 * itself: `BLUETOOTH_CONNECT` on Android 12+, nothing at runtime before that (the legacy
 * `BLUETOOTH` permission is install-time).
 */
public object BluetoothPermissions {
    /** Runtime permissions for the given API level. */
    public fun required(sdkInt: Int = Build.VERSION.SDK_INT): List<String> =
        if (sdkInt >= Build.VERSION_CODES.S) listOf(Manifest.permission.BLUETOOTH_CONNECT) else emptyList()

    /** The subset of [required] not yet granted. */
    public fun missing(
        context: Context,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ): List<String> =
        required(sdkInt).filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
}

package com.example.pugprint.bluetooth

import com.example.pugprint.printer.transport.PrinterDevice
import com.juul.kable.Peripheral
import com.juul.kable.toIdentifier

/** Creates the Kable [Peripheral] for a paired printer; swapped for a mock in tests. */
public fun interface PeripheralFactory {
    public fun create(device: PrinterDevice): Peripheral
}

/**
 * Real-device factory. The printer is addressed by the MAC learned at pairing time
 * ([PrinterDevice.id]); Android's stack finds it without a scan, so no scan permission is needed.
 */
public object HelloBlinkPeripherals : PeripheralFactory {
    /**
     * Requested ATT MTU. Android's default 23 allows 20-byte writes — too small for the 56-byte
     * single-row `GS v 0` block. 247 is what the spike negotiated (245-byte payload) and is
     * accepted by the printer's module.
     */
    public const val REQUESTED_MTU: Int = 247

    override fun create(device: PrinterDevice): Peripheral =
        Peripheral(device.id.uppercase().toIdentifier()) {
            onServicesDiscovered { requestMtu(REQUESTED_MTU) }
            // A failed notification subscription is not fatal on its own; link loss is
            // reported through the peripheral's state, which BleTransport watches.
            observationExceptionHandler { }
        }
}

package com.example.pugprint.printer

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.pugprint.printer.transport.PrinterDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Remembers the one printer the user paired so the app reconnects to it on launch. */
interface PairedPrinterStore {
    fun load(): PrinterDevice?

    fun save(device: PrinterDevice)

    fun clear()
}

/** Address and name only, in app-private preferences; nothing else is stored. */
@Singleton
class PreferencesPairedPrinterStore
    @Inject
    constructor(
        @ApplicationContext context: Context,
    ) : PairedPrinterStore {
        private val prefs: SharedPreferences = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)

        override fun load(): PrinterDevice? {
            val id = prefs.getString(KEY_ID, null) ?: return null
            return PrinterDevice(id, prefs.getString(KEY_NAME, null))
        }

        override fun save(device: PrinterDevice) {
            prefs.edit {
                putString(KEY_ID, device.id)
                putString(KEY_NAME, device.name)
            }
        }

        override fun clear() {
            prefs.edit { clear() }
        }

        private companion object {
            const val FILE = "paired_printer"
            const val KEY_ID = "id"
            const val KEY_NAME = "name"
        }
    }

/** For tests and the fake-printer build. */
class InMemoryPairedPrinterStore(
    private var device: PrinterDevice? = null,
) : PairedPrinterStore {
    override fun load(): PrinterDevice? = device

    override fun save(device: PrinterDevice) {
        this.device = device
    }

    override fun clear() {
        device = null
    }
}

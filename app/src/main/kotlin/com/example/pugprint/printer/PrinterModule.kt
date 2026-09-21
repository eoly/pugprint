package com.example.pugprint.printer

import com.example.pugprint.BuildConfig
import com.example.pugprint.bluetooth.BleTransport
import com.example.pugprint.bluetooth.HelloBlinkPeripherals
import com.example.pugprint.printer.transport.FakePrinterTransport
import com.example.pugprint.printer.transport.PrinterClient
import com.example.pugprint.printer.transport.PrinterTransport
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

/** Process-wide scope for the printer connection; outlives any screen. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope

/**
 * Printer wiring. `BuildConfig.FAKE_PRINTER` (Gradle `-Ppugprint.fakePrinter=true`) swaps the
 * BLE stack for the in-process emulator so the flow runs on an emulator without Bluetooth.
 */
@Module
@InstallIn(SingletonComponent::class)
object PrinterModule {
    @Provides
    @Singleton
    @ApplicationScope
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun transport(
        @ApplicationScope scope: CoroutineScope,
    ): PrinterTransport =
        if (BuildConfig.FAKE_PRINTER) FakePrinterTransport() else BleTransport(HelloBlinkPeripherals, scope)

    @Provides
    @Singleton
    fun client(transport: PrinterTransport): PrinterClient = PrinterClient(transport)

    @Provides
    @Singleton
    fun pairing(companion: CompanionPrinterPairing): PrinterPairing =
        if (BuildConfig.FAKE_PRINTER) FakePrinterPairing() else companion

    @Provides
    @Singleton
    fun permission(bluetooth: BluetoothConnectPermission): ConnectPermission =
        if (BuildConfig.FAKE_PRINTER) ConnectPermission { true } else bluetooth

    @Provides
    @Singleton
    fun store(preferences: PreferencesPairedPrinterStore): PairedPrinterStore = preferences

    @Provides
    fun backoff(): ReconnectBackoff = ReconnectBackoff.EXPONENTIAL
}

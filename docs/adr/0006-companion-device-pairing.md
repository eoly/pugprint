# 6. Pair through the Companion Device Manager; request only BLUETOOTH_CONNECT
Date: 2026-09-21 · Status: Accepted

## Context
Phase 3 adds the first Bluetooth code path. Android offers two ways to find a BLE printer:
scan from the app (`BLUETOOTH_SCAN` on Android 12+, plus `ACCESS_FINE_LOCATION` before that,
and a runtime prompt either way) or ask the **Companion Device Manager (CDM)** to scan and
show the system picker. PugPrint is a kids' app with a no-location, minimal-permission policy
(`docs/PRIVACY_POLICY.md`, `CLAUDE.md`), and there is exactly one printer to find.

## Decision
- Pairing goes through CDM: an `AssociationRequest` with a `BluetoothLeDeviceFilter` on the
  Hello Blink UART service `49535343-fe7d-4ae5-8fa9-9fafd205e455`. The system scans, the
  user picks `HB-nnnn` from the picker, and the app receives the device address.
- The app remembers that one address (`PairedPrinterStore`, app-private preferences) and
  connects to it directly by MAC through Kable. No app-side scanning, ever.
- Manifest permissions: `BLUETOOTH` (`maxSdkVersion=30`, install-time) and
  `BLUETOOTH_CONNECT` (runtime on Android 12+). **No** `BLUETOOTH_SCAN`, **no** location.
- Features: `bluetooth_le` required; `companion_device_setup` declared but not required so
  Play still lists the app on devices where it is missing (the app then reports pairing as
  unavailable rather than crashing).
- `:core:bluetooth` is the only module that imports Android Bluetooth APIs. It contains no
  protocol bytes; those stay in `:core:printer`.

## Consequences
- One runtime prompt ("Nearby devices") on Android 12+, none before. No location dialog.
- The stored address is the only persisted state in the app. "Forget printer" clears it.
- Reconnecting by address needs the printer to be advertising; the connection loop retries
  with exponential backoff (1 s → 30 s cap) until the user forgets the printer. Battery cost
  of indefinite retries is to be evaluated in Phase 5 (stop retrying in the background).
- Emulators have no Bluetooth and often no CDM; the `-Ppugprint.fakePrinter=true` build
  substitutes `FakePrinterTransport` + instant pairing so the print flow can still be driven.
- Fallback if CDM proves unreliable on some phones: an app-side Kable `Scanner` behind the
  same `PrinterPairing` interface, which would reintroduce `BLUETOOTH_SCAN`
  (`neverForLocation`) and needs a revision of this ADR.

# 5. Print over BLE transparent UART, not Bluetooth Classic RFCOMM
Date: 2026-09-20 · Status: Accepted

## Context
The Phase 0 spike showed the Hello Blink is a dual-mode printer. The official app prints
over Bluetooth Classic SPP/RFCOMM (`00001101-…`), streaming the whole raster in one write.
The printer also exposes a Microchip transparent-UART GATT service over BLE, which reaches
the same command parser. Over BLE, a raster block larger than one write is dropped, and
2- or 4-row blocks lose about half their rows even with 40–100 ms pauses; 1-row blocks
print correctly.

## Decision
Use **BLE** (Kable, per ADR 3) against the `49535343-fe7d-…` UART service. Send each
`GS v 0` block in a single write-without-response, 1 row per block, ~20 ms apart (raise
only after a hardware test proves it). Keep the protocol layer transport-agnostic so an
RFCOMM `PrinterTransport` can be added if needed.

## Consequences
- Modern permission model (BLUETOOTH_SCAN/CONNECT + Companion Device Manager), no legacy
  discovery or location permission — matches the kids'-app privacy goals.
- Slower than RFCOMM: at 1 row/block with ~70 ms per block as used in the spike, a
  384×384 image takes ~27 s; with 20 ms pacing ≈ 8 s. Tune in Phase 3 and show progress
  in the UI. The printer's receive buffer, not BLE bandwidth, is the limit.
- iOS port later would need BLE anyway (no MFi for SPP).
- Risk: BLE block-size behaviour is firmware-dependent. Fallback is an RFCOMM transport
  behind the same `PrinterTransport` interface.

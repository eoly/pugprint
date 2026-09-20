# Roadmap

- **Phase 0 — Protocol discovery spike (Mac):** nRF Connect scan; `bleak` Python client;
  Android HCI snoop + Wireshark; jadx of the official APK. Output: confirmed UUIDs, command
  bytes, energy defaults, ABIs/targetSdk → written into PRINTER_PROTOCOL.md + golden fixtures.
- **Phase 1 — Skeleton + CI:** Gradle project, version catalog, modules, ktlint/detekt,
  GitHub Actions green.
- **Phase 2 — Protocol library:** `:core:printer` with full golden tests (no hardware).
- **Phase 3 — BLE transport:** Kable `BleTransport`, CDM pairing, real-device printing.
- **Phase 4 — Image pipeline + UI:** `:core:imaging` dithering, Photo Picker, crop/rotate,
  preview, print flow.
- **Phase 5 — Polish + kid features:** text/stamps/drawing, density slider, error UX,
  accessibility, large touch targets.
- **Phase 6 — Play:** internal track to the friend group → (if going public) closed test
  (12 testers/14 days) → production.

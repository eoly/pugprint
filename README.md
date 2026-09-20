# PugPrint

Native Android app to print photos and doodles to the "Hello Blink" mini thermal
printer (a rebranded 58 mm "cat printer"). Built with Kotlin + Jetpack Compose.
**Offline. No accounts. No data collection.**

> PugPrint is an independent, unofficial app. It is not affiliated with or endorsed
> by Buffalo Games, Ceaco, or the makers of Hello Blink. "Hello Blink" is a
> trademark of its owner. PugPrint is *compatible with* Hello Blink and similar
> 58 mm Bluetooth mini printers.

## Status
Pre-alpha. Phase 0 (protocol discovery) is done: the printer protocol is **confirmed** on
real hardware and a test image has been printed from a Python script over BLE — see
`docs/PRINTER_PROTOCOL.md`. Next: Phase 1 (Gradle skeleton + CI).

## Quick start
```
./gradlew assembleDebug          # build
./gradlew testDebugUnitTest      # JVM unit tests
./gradlew ktlintCheck detekt lint
```
Real printing requires a **physical Android device** — the emulator has no Bluetooth.
Most development runs against `FakePrinterTransport` in the emulator/JVM.

## License
MIT — see `LICENSE`.

## Docs
`docs/PRD.md` · `docs/ARCHITECTURE.md` · `docs/PRINTER_PROTOCOL.md` · `docs/PHASE0_SPIKE.md` ·
`docs/DEV_ENVIRONMENT.md` · `docs/TESTING.md` · `docs/SDLC.md` ·
`docs/RELEASE.md` · `docs/PRIVACY_POLICY.md` · `docs/ROADMAP.md` · `docs/adr/`

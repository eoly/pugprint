# PugPrint

Native Android app to print photos and doodles to the "Hello Blink" mini thermal
printer (a rebranded 58 mm "cat printer"). Built with Kotlin + Jetpack Compose.
**Offline. No accounts. No data collection.**

> PugPrint is an independent, unofficial app. It is not affiliated with or endorsed
> by Buffalo Games, Ceaco, or the makers of Hello Blink. "Hello Blink" is a
> trademark of its owner. PugPrint is *compatible with* Hello Blink and similar
> 58 mm Bluetooth mini printers.

## Status
Pre-alpha. The printer protocol is INFERRED from the cat-printer family and must be
confirmed in the Phase 0 discovery spike — see `docs/PRINTER_PROTOCOL.md`.

## Quick start
```
./gradlew assembleDebug          # build
./gradlew testDebugUnitTest      # JVM unit tests
./gradlew ktlintCheck detekt lint
```
Real printing requires a **physical Android device** — the emulator has no Bluetooth.
Most development runs against `FakePrinterTransport` in the emulator/JVM.

## Docs
`docs/PRD.md` · `docs/ARCHITECTURE.md` · `docs/PRINTER_PROTOCOL.md` ·
`docs/DEV_ENVIRONMENT.md` · `docs/TESTING.md` · `docs/SDLC.md` ·
`docs/RELEASE.md` · `docs/PRIVACY_POLICY.md` · `docs/ROADMAP.md` · `docs/adr/`

# 2. Native Kotlin + Jetpack Compose (vs Flutter / React Native / KMP)
Date: 2026-09-20 · Status: Accepted

## Context
Android-only today; BLE-heavy; image processing; developed largely via Claude Code;
possible iOS later.

## Decision
Use native Kotlin + Compose.

## Consequences
- BLE is first-class (Kable/Nordic) with no plugin/bridge indirection — decisive for a
  reverse-engineered GATT protocol.
- Image processing (dithering, 1bpp packing) is simple pure-Kotlin, unit-testable on the JVM.
- Claude Code drives Gradle/adb/logcat natively; the whole toolchain is text/CLI-friendly.
- Flutter/RN would add a BLE-plugin abstraction over exactly the tricky part; RN/Flutter also
  historically add native `.so`s that raise 16 KB-page concerns.
- If iOS is needed later, migrate `:core:printer`/`:core:imaging` to KMP (they're already
  pure Kotlin) and keep native UIs. Revisit then.

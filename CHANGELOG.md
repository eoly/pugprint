# Changelog

All notable changes to PugPrint are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and versions follow SemVer.

## [Unreleased]

### Added
- Phase 1 skeleton: Gradle 8.13 wrapper, version catalog, `:app` (Compose + Hilt),
  pure-JVM `:core:printer` and `:core:imaging`, ktlint/detekt/Kover, Roborazzi golden
  screenshot of the placeholder home screen, GitHub Actions CI.
- `MonoBitmap` 1bpp packing (MSB-first, 1 = black) with tests.
- Printer GATT UUIDs and head constants (384 dots, 48 bytes/row) with tests.

### Changed
- Phase 0 protocol discovery documented in `docs/PRINTER_PROTOCOL.md`.
- JUnit 5.14 → JUnit 6.1 (platform, Jupiter, vintage moved to a single version).
- Dependabot ignores the packages that need AGP 9 / compileSdk 37 until the platform bump.

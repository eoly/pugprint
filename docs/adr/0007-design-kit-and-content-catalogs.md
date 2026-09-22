# 7. Design kit module and content catalogs
Date: 2026-09-22 · Status: Accepted

## Context
Phase 5 adds polish and kid features (themes, stamps, text, drawing, friendlier errors). A later
phase hands the *design* of the app to a kid designer: she should be able to change colours,
shapes, layout and add fun content (themes, stamps, fonts) with a parent's help, without
touching printing or Bluetooth code, and without the goldens that guard those layers noticing.

Today the look lives in one `Theme.kt` plus ad-hoc Material calls inside every screen
(`FilterChip`, `TextButton`, raw `dp` values, `MaterialTheme.colorScheme.error`). Changing the
look means editing every screen; adding a theme has nowhere to go.

## Decision
1. **A design-kit module, `:ui:design`** (Android library, Compose only). It depends on nothing
   in this repo and nothing in this repo depends on it except `:app`. It holds:
   - **Tokens**: `PugTheme` (a palette of ~12 named colours, a `roundness`, a font slot),
     `PugSpacing`, `PugTouch` (64 dp primary / 56 dp secondary / 48 dp minimum targets) and
     `PugLayout` (content width capped so the tablet layout stays kid-sized).
   - **The theme catalog**: `ThemeCatalog.all`. Adding a theme is adding one `PugTheme(...)`
     entry; nothing else registers it. Tests enumerate the catalog: contrast ratios for every
     text/background pair are asserted (WCAG AA), and a Roborazzi gallery golden is recorded
     per theme automatically.
   - **Kit components** screens are built from: `KidScreen`, `HeroTitle`, `BigButton`,
     `ChoiceRow`, `StatusBanner`. Screens in `:app` use these instead of raw Material widgets,
     so layout and look change in one place.
2. **Content catalogs, one file each, enumerated by tests.** Themes now; stamps, fonts and
   sticker shapes follow the same shape: a list in one file, data only, with a golden test that
   walks the list. "Add a thing = add a line (and maybe a bitmap)".
3. **Sticker content stays pure JVM.** Text, stamps and drawings become layers of a `Sticker`
   model rendered by `:core:imaging` (golden PBMs), so a new fun feature is a new layer or tool
   and never touches `:core:printer`.
4. **Features stay packages in `:app`** (`ui/home`, `ui/editor`, `ui/draw` …) rather than
   `:feature:*` modules; ADR 0004's deferral stands. The kit is the boundary worth a module
   because it is the boundary the designer works inside.

## Consequences
- Screens get simpler (state → kit components); the goldens for `:app` are re-recorded once.
- Every theme is checked for readability before it ships; the designer sees her theme in the
  gallery goldens on the first build.
- One more Android library module (~15 s of extra build). `:ui:design` must not import
  `:core:*` — reviewed by hand, enforced by its `build.gradle.kts` having no such dependency.
- `docs/DESIGN_KIT.md` is the designer's handbook and is kept in step with the kit.

# SDLC / Contributing

## Branching
Trunk-based: short-lived `feat/…`, `fix/…` branches off `main`; squash-merge via PR.

## Commits
Conventional Commits: `feat:`, `fix:`, `docs:`, `test:`, `chore:`, `refactor:`.

## Versioning
- `versionName` = SemVer (MAJOR.MINOR.PATCH).
- `versionCode` = CI build number (must be strictly increasing; Play rejects duplicates).
- Maintain `CHANGELOG.md` (Keep a Changelog).

## Definition of Done
- [ ] Unit + golden tests pass; new code has tests.
- [ ] `ktlintCheck detekt lint` clean.
- [ ] Roborazzi screenshots updated if UI changed.
- [ ] CHANGELOG updated; PR template checklist complete.
- [ ] No new analytics/network/permissions without an ADR.

## Release process
Tag `vX.Y.Z` → CI builds signed AAB → uploads to Play **internal** track → promote to
closed/production per `docs/RELEASE.md`.

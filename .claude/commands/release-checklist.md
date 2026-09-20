---
description: Verify the repo is release-ready
allowed-tools: Read, Bash, Grep
---
Check and report on: versionCode bumped, CHANGELOG updated, tests + lint + detekt clean,
targetSdk == 36, no analytics/network deps added, keystore.properties gitignored, and that
`./gradlew bundleRelease` produces a signed AAB. List anything missing.

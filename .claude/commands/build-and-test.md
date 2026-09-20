---
description: Build debug, run unit tests + lint, and fix any failures
allowed-tools: Bash, Read, Edit
---
Run `./gradlew assembleDebug testDebugUnitTest ktlintCheck detekt lint`.
If anything fails, read the report, fix the root cause, and re-run until green.
Report a short summary of what failed and how you fixed it.

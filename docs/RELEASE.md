# Release & Publishing

## Signing
- Use **Play App Signing** (Google holds the app signing key).
- You hold the **upload key** in a keystore referenced by `keystore.properties`
  (GITIGNORED). CI reads the keystore + passwords from GitHub secrets.

## Tracks & the new-account gate
- **Internal testing** — up to **100 testers**, instant, NO 12/14 requirement. START HERE.
- **Closed testing** — REQUIRED for new personal accounts: ≥12 testers opted in for
  14 continuous days before you can apply for production. (Org accounts are exempt.)
- **Production** — public. Only after closed-testing gate + review.

## Automated upload (CI)
On a `v*.*.*` tag, CI runs `./gradlew bundleRelease` and uploads via
`r0adkll/upload-google-play` (or `fastlane supply`) using a Play **service-account JSON**
stored as `PLAY_SERVICE_ACCOUNT_JSON`. Derive `versionCode` from the build number.

## Pre-submission checklist
- [ ] targetSdk 36; AAB (not APK).
- [ ] Privacy policy URL live (see PRIVACY_POLICY.md).
- [ ] Data safety form: "No data collected", "No data shared".
- [ ] Content rating questionnaire completed.
- [ ] Target audience = children; **Families** program opted in; only certified/zero ad SDKs.
- [ ] Store listing avoids "Hello Blink" as a name; use "compatible with…".
- [ ] 16 KB: pure-Kotlin app is compliant; re-check if any native lib is added.

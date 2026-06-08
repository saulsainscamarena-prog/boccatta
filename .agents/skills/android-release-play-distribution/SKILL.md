---
name: android-release-play-distribution
description: Use this skill for Android release and Google Play distribution in Bocatta POS, including App Bundles, versioning, signing readiness, R8/minify, internal testing, Play Console checks, and production-safe build config.
metadata:
  keywords:
  - android
  - release
  - google play
  - app bundle
  - r8
  - signing
---

# Android Release And Play Distribution

Use this when preparing a release, app bundle, internal test build, signing check, version bump, or Play Console readiness pass.

## Bocatta Context

Relevant files:

- `app/build.gradle.kts`
- `app/proguard-rules.pro`
- `gradle/libs.versions.toml`
- `firestore.rules`
- `firestore.indexes.json`
- `R8_Configuration_Analysis.md`

## Workflow

1. Confirm release build config differs safely from debug config.
2. Check `versionCode`, `versionName`, minify, resource shrinking, and BuildConfig demo flags.
3. Validate R8 keep rules for Firebase, serialization, Room/KSP, and any reflective model mapping.
4. Confirm Firestore rules/indexes are aligned with the release app behavior.
5. Prefer App Bundle for Play distribution.
6. Run a smoke test of checkout, offline save, sync, reports, and login before release.
7. Utilize the R8 Configuration Analyzer in Android Studio to evaluate keep rules and optimization potential.
8. Ensure mapping.txt is uploaded to the Play Console for proper deobfuscation of crash reports.

## Guardrails

- Do not ship demo credentials or demo mode in release.
- Do not disable minify/resource shrinking to hide R8 problems unless explicitly approved.
- Do not publish without checking offline-critical flows.

## Verification

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat compileDebugKotlin
```

## Web Research Directive
**Last Verified Date:** 2026-06-08

If the `Last Verified Date` above is more than 30 days older than the current date, you MUST use the `search_web` tool to search the official documentation at **docs.gradle.org** or **developer.android.com** (Google for Developers) to ensure that the APIs, configurations, and best practices are up-to-date with the latest stable releases. Do not rely solely on your pre-training data. If you perform this verification, you MUST update the `Last Verified Date` in this file to the current date to prevent redundant web searches in future executions.

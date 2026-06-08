---
name: android-performance-baseline-profiles
description: Use this skill for Android runtime performance and Baseline Profiles in Bocatta POS, including startup, checkout journeys, sales grid navigation, release-mode measurement, and low-end tablet smoothness.
metadata:
  keywords:
  - android
  - performance
  - baseline profiles
  - startup
  - macrobenchmark
  - release
---

# Android Performance And Baseline Profiles

Use this when optimizing startup, first sale time, navigation smoothness, or release performance.

## Bocatta Context

High-value journeys:

- App launch to login/session restoration.
- Login to sales screen.
- Add products, edit cart, split payment, checkout.
- Offline sale save and later sync.
- Inventory opening/closing.

## Workflow

1. Confirm the issue in release-like conditions; debug builds distort Compose and startup performance.
2. Separate Compose recomposition problems from app startup/runtime compilation problems.
3. Add Baseline Profiles only after identifying stable critical journeys.
4. Keep profile generation deterministic and independent from production credentials.
5. Review R8/minify interactions for release builds.

## Guardrails

- Do not claim performance wins from debug-only runs.
- Do not add large dependencies for measurement without checking existing Gradle setup.
- Do not optimize away audit/logging paths required for POS safety.

## Verification

At minimum:

```powershell
.\gradlew.bat assembleDebug
```

For release/profile work, also verify the relevant managed-device or benchmark setup if present.

## Web Research Directive
**Last Verified Date:** 2026-06-07

If the `Last Verified Date` above is more than 30 days older than the current date, you MUST use the `search_web` tool to search the official documentation at **docs.gradle.org** or **developer.android.com** (Google for Developers) to ensure that the APIs, configurations, and best practices are up-to-date with the latest stable releases. Do not rely solely on your pre-training data. If you perform this verification, you MUST update the `Last Verified Date` in this file to the current date to prevent redundant web searches in future executions.

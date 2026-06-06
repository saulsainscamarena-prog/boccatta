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

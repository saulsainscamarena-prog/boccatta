---
name: android-crash-reporting-observability
description: Use this skill for crash reporting, logging, observability, Timber, Firebase Analytics, local log cleanup, sync error reporting, and operator-safe diagnostics in Bocatta POS.
metadata:
  keywords:
  - android
  - crash reporting
  - logging
  - timber
  - analytics
  - observability
---

# Android Crash Reporting And Observability

Use this when changing logs, analytics, crash diagnostics, error reporting, or cleanup of diagnostic data.

## Bocatta Context

Relevant files:

- `app/src/main/java/com/bocatta/pos/logging/LogHelper.kt`
- `app/src/main/java/com/bocatta/pos/logging/LogCleanupWorker.kt`
- `app/src/main/java/com/bocatta/pos/data/repository/SyncErrorRepositoryImpl.kt`
- `app/src/main/java/com/bocatta/pos/domain/model/SyncError.kt`
- `app/src/main/java/com/bocatta/pos/BocattaApp.kt`
- `app/build.gradle.kts`

## Workflow

1. Decide whether the event is user-facing, diagnostic, analytic, or audit-critical.
2. Use existing logging helpers and repositories before adding new telemetry.
3. Never log credentials, raw PINs, secrets, or full sensitive employee/customer data.
4. Include enough context to debug sync/offline failures without leaking private data.
5. Keep log cleanup compatible with offline troubleshooting.
6. If adding Crashlytics or analytics events, verify dependencies and initialization first.
7. Use the App Quality Insights tool window in Android Studio to view Firebase Crashlytics data directly within your IDE.
8. Integrate Firebase Analytics to automatically collect crash breadcrumbs that provide a trail of user actions leading up to an event.

## Guardrails

- Do not add silent catches around checkout, sync, inventory, or auth.
- Do not rely on analytics as the only audit record.
- Do not initialize Firebase services in multiple places.

## Verification

```powershell
.\gradlew.bat compileDebugKotlin
```

## Web Research Directive
**Last Verified Date:** 2026-06-08

If the `Last Verified Date` above is more than 30 days older than the current date, you MUST use the `search_web` tool to search the official documentation at **docs.gradle.org** or **developer.android.com** (Google for Developers) to ensure that the APIs, configurations, and best practices are up-to-date with the latest stable releases. Do not rely solely on your pre-training data. If you perform this verification, you MUST update the `Last Verified Date` in this file to the current date to prevent redundant web searches in future executions.

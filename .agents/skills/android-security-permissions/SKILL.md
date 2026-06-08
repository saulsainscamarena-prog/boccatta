---
name: android-security-permissions
description: Use this skill for Android security and permissions in Bocatta POS, including manifest review, exported components, least-privilege permissions, Firebase rules impact, sensitive POS data, and anti-fraud surfaces.
metadata:
  keywords:
  - android
  - security
  - permissions
  - manifest
  - firebase rules
  - pos
---

# Android Security And Permissions

Use this when changing permissions, manifest entries, exported components, authentication gates, audit logs, or sensitive operational data.

## Bocatta Context

Start with:

- `app/src/main/AndroidManifest.xml`
- `firestore.rules`
- `app/src/main/java/com/bocatta/pos/domain/usecase/AuthorizationManager.kt`
- `app/src/main/java/com/bocatta/pos/data/repository/AuthRepository.kt`
- `app/src/main/java/com/bocatta/pos/core/constants/FirestoreCollections.kt`

## Workflow

1. Confirm the user-facing reason for each permission or exported component.
2. Prefer least privilege and avoid broad storage/location/device permissions.
3. Check whether Firestore rules and app-side authorization agree.
4. Preserve audit records for cancelations, employee changes, inventory movements, and payroll-related operations.
5. Keep secrets out of source, Gradle files, logs, and generated reports.
6. Review fallback behavior when auth or role data cannot be loaded.

## Guardrails

- Do not add new Firebase initialization paths without checking existing providers and DI.
- Do not trust client-side role checks as the only protection for remote data.
- Do not make activities/services exported unless required.

## Verification

Inspect manifest and run:

```powershell
.\gradlew.bat compileDebugKotlin
```

## Web Research Directive
**Last Verified Date:** 2026-06-07

If the `Last Verified Date` above is more than 30 days older than the current date, you MUST use the `search_web` tool to search the official documentation at **docs.gradle.org** or **developer.android.com** (Google for Developers) to ensure that the APIs, configurations, and best practices are up-to-date with the latest stable releases. Do not rely solely on your pre-training data. If you perform this verification, you MUST update the `Last Verified Date` in this file to the current date to prevent redundant web searches in future executions.

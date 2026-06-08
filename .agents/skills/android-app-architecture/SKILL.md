---
name: android-app-architecture
description: Use this skill for Bocatta POS Android architecture decisions, including ViewModels, repositories, use cases, Koin modules, domain/data boundaries, Firestore providers, and avoiding unnecessary new architecture.
metadata:
  keywords:
  - android
  - architecture
  - viewmodel
  - repository
  - use case
  - koin
---

# Android App Architecture

Use this when a change crosses UI, ViewModel, domain, repository, DI, or Firebase/local data boundaries.

## Bocatta Context

Start with:

- `app/src/main/java/com/bocatta/pos/di/AppModules.kt`
- `app/src/main/java/com/bocatta/pos/di/SalesDependencies.kt`
- `app/src/main/java/com/bocatta/pos/domain/usecase/*.kt`
- `app/src/main/java/com/bocatta/pos/domain/repository/*.kt`
- `app/src/main/java/com/bocatta/pos/data/repository/*.kt`
- `app/src/main/java/com/bocatta/pos/presentation/viewmodel/*.kt`

## Workflow

1. Identify the existing owner of the behavior before adding a new class.
2. Keep business rules in domain/use cases/repositories, not Composables.
3. Keep UI state in ViewModels.
4. Reuse Koin modules and local provider patterns before introducing new DI wiring.
5. Update contracts and tests when changing repository or use case signatures.
6. Keep changes small and local unless the current boundary is the actual cause.

## Guardrails

- Do not create parallel repositories for the same collection/domain.
- Do not instantiate Firebase/Room dependencies in UI when a provider/repository exists.
- Do not refactor broad architecture while fixing a narrow bug.

## Verification

```powershell
.\gradlew.bat testDebugUnitTest --tests "*Koin*"
.\gradlew.bat compileDebugKotlin
```

## Web Research Directive
**Last Verified Date:** 2026-06-07

If the `Last Verified Date` above is more than 30 days older than the current date, you MUST use the `search_web` tool to search the official documentation at **docs.gradle.org** or **developer.android.com** (Google for Developers) to ensure that the APIs, configurations, and best practices are up-to-date with the latest stable releases. Do not rely solely on your pre-training data. If you perform this verification, you MUST update the `Last Verified Date` in this file to the current date to prevent redundant web searches in future executions.

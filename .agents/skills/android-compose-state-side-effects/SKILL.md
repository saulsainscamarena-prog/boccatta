---
name: android-compose-state-side-effects
description: Use this skill for Jetpack Compose state and side effects in Bocatta POS, including snackbars, dialogs, one-shot events, success flags, modal close behavior, LaunchedEffect, collectAsStateWithLifecycle, and unidirectional state.
metadata:
  keywords:
  - android
  - compose
  - state
  - side effects
  - events
  - dialogs
---

# Android Compose State And Side Effects

Use this when fixing UI state, transient events, dialogs, snackbars, navigation effects, or lifecycle-aware state collection.

## Bocatta Context

Common files:

- `app/src/main/java/com/bocatta/pos/presentation/viewmodel/*ViewModel.kt`
- `app/src/main/java/com/bocatta/pos/presentation/ui/screens/**/*.kt`
- `app/src/main/java/com/bocatta/pos/presentation/ui/components/*.kt`
- `app/src/main/java/com/bocatta/pos/presentation/viewmodel/BaseViewModel.kt`

## Workflow

1. Identify durable UI state versus one-shot events.
2. Keep durable state in ViewModel state objects or flows.
3. Consume one-shot events after display so snackbars/dialogs do not repeat on recomposition.
4. Use `collectAsStateWithLifecycle` for lifecycle-aware Flow collection.
5. Use `LaunchedEffect(key)` for effects caused by state transitions.
6. Close modals after successful operations and reset any success flags.
7. Defer state reads to the lowest possible scope (e.g., using lambda-based modifiers) to minimize unnecessary recompositions.
8. Ensure Composables are idempotent and do not write to state backward during composition to avoid infinite loops.

## Guardrails

- Do not launch business operations directly from Composables if the ViewModel already owns the action.
- Do not use raw mutable globals for UI events.
- Do not leave dialogs open after completed save/delete/checkout flows.

## Verification

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat compileDebugKotlin
```

## Web Research Directive
**Last Verified Date:** 2026-06-08

If the `Last Verified Date` above is more than 30 days older than the current date, you MUST use the `search_web` tool to search the official documentation at **docs.gradle.org** or **developer.android.com** (Google for Developers) to ensure that the APIs, configurations, and best practices are up-to-date with the latest stable releases. Do not rely solely on your pre-training data. If you perform this verification, you MUST update the `Last Verified Date` in this file to the current date to prevent redundant web searches in future executions.

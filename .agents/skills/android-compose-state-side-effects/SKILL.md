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

## Guardrails

- Do not launch business operations directly from Composables if the ViewModel already owns the action.
- Do not use raw mutable globals for UI events.
- Do not leave dialogs open after completed save/delete/checkout flows.

## Verification

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat compileDebugKotlin
```

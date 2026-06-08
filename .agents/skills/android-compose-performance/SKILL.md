---
name: android-compose-performance
description: Use this skill to optimize Jetpack Compose performance in Bocatta POS, especially SalesScreen, product grids, cart panels, derived totals, Lazy layouts, recomposition, and jank during fast counter operations.
metadata:
  keywords:
  - android
  - compose
  - performance
  - recomposition
  - lazy list
  - bocatta
---

# Android Compose Performance

Use this when a Compose screen feels slow, recomposes too broadly, or handles frequently changing sale/cart state.

## Bocatta Context

Start with:

- `app/src/main/java/com/bocatta/pos/presentation/ui/screens/ventas/SalesScreen.kt`
- `app/src/main/java/com/bocatta/pos/presentation/ui/components/CarritoPanelV2.kt`
- `app/src/main/java/com/bocatta/pos/presentation/viewmodel/SalesViewModelV2.kt`
- `app/src/main/java/com/bocatta/pos/presentation/viewmodel/SalesUiState.kt`
- `app/src/main/java/com/bocatta/pos/domain/util/CarritoCalculator.kt`

## Workflow

1. Identify hot state: cart items, selected category, filters, totals, product grids, and dialogs.
2. Keep heavy calculations in ViewModels/use cases/domain utilities.
3. Use immutable lists/maps for UI state and stable keys in lazy grids/lists.
4. Use `derivedStateOf` only for cheap UI-derived values that reduce recomposition.
5. Defer state reads to the smallest composable that needs them.
6. Avoid passing entire large state objects to every product tile or cart row.

## Guardrails

- Do not move pricing, tax, inventory, or checkout rules into Composables.
- Do not introduce broad remembered mutable state that competes with the ViewModel.
- Do not redesign the sales flow before measuring or inspecting state reads.

## Verification

Run Compose/UI tests when touched, then compile:

```powershell
.\gradlew.bat connectedDebugAndroidTest
.\gradlew.bat compileDebugKotlin
```

## Web Research Directive
Before executing any actions based on this skill, you MUST use the `search_web` tool to search the official documentation at **docs.gradle.org** or **developer.android.com** (Google for Developers). Ensure that the APIs, Gradle configurations, and best practices you are about to apply are completely up-to-date with the latest stable releases. Do not rely solely on your pre-training data.

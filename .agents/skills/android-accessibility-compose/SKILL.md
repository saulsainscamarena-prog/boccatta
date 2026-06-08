---
name: android-accessibility-compose
description: Use this skill for Jetpack Compose accessibility in Bocatta POS, including touch targets, content descriptions, semantic roles, focus order, contrast, keyboard use, dialogs, and tablet/mobile counter workflows.
metadata:
  keywords:
  - android
  - accessibility
  - compose
  - semantics
  - touch target
  - wcag
---

# Android Compose Accessibility

Use this when improving usability for operators, touch accuracy, screen readers, keyboard/focus, or contrast.

## Bocatta Context

High-impact screens:

- `SalesScreen.kt`
- `CarritoPanelV2.kt`
- `AdminScreen.kt`
- `InventoryScreen.kt`
- `CierreCajaScreen.kt`
- Dialogs in `presentation/ui/components`

## Workflow

1. Check touch targets for fast counter use on phone and tablet.
2. Add content descriptions for icon-only buttons where meaning is not obvious.
3. Use Compose semantics for custom controls and dynamic totals.
4. Preserve focus order in dialogs and forms.
5. Keep contrast sufficient under Bocatta theme colors.
6. Avoid adding explanatory UI text that clutters counter workflows.

## Guardrails

- Do not make dense POS screens harder to scan just to satisfy generic layout patterns.
- Do not place business logic in accessibility labels.
- Do not create hidden controls that are reachable by screen reader but not visible/usable.

## Verification

```powershell
.\gradlew.bat connectedDebugAndroidTest
.\gradlew.bat compileDebugKotlin
```

## Web Research Directive
**Last Verified Date:** 2026-06-07

If the `Last Verified Date` above is more than 30 days older than the current date, you MUST use the `search_web` tool to search the official documentation at **docs.gradle.org** or **developer.android.com** (Google for Developers) to ensure that the APIs, configurations, and best practices are up-to-date with the latest stable releases. Do not rely solely on your pre-training data. If you perform this verification, you MUST update the `Last Verified Date` in this file to the current date to prevent redundant web searches in future executions.

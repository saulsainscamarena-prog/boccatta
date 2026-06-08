---
name: android-user-identity-auth
description: Use this skill for user identity, Firebase Auth, roles, employee authorization, PIN/admin flows, session state, and auditable access control in Bocatta POS.
metadata:
  keywords:
  - android
  - auth
  - firebase auth
  - identity
  - roles
  - authorization
---

# Android User Identity And Auth

Use this when touching login, sessions, employees, roles, permissions, admin PINs, or user audit flows.

## Bocatta Context

Relevant files:

- `app/src/main/java/com/bocatta/pos/presentation/viewmodel/AuthViewModelV2.kt`
- `app/src/main/java/com/bocatta/pos/presentation/viewmodel/SessionViewModel.kt`
- `app/src/main/java/com/bocatta/pos/data/repository/AuthRepository.kt`
- `app/src/main/java/com/bocatta/pos/domain/usecase/AuthorizationManager.kt`
- `app/src/main/java/com/bocatta/pos/presentation/ui/components/AdminPinDialog.kt`
- `app/src/test/java/com/bocatta/pos/domain/usecase/AuthorizationManagerTest.kt`

## Workflow

1. Trace the full path: UI action, ViewModel, repository/use case, Firebase, local state.
2. Keep role and permission decisions auditable.
3. Handle auth failures explicitly with visible UI state.
4. Avoid leaking Firebase exceptions or credentials into logs.
5. Do not let demo credentials affect release builds.
6. Keep admin overrides narrow and time-bound when possible.
7. Favor modern authentication such as Credential Manager to unify passkeys, passwords, and federated sign-in.
8. Avoid hardware-specific identifiers for users; strictly use resettable identifiers to preserve privacy.

## Guardrails

- Do not put authorization logic inside Composables.
- Do not delete employee/user records without an audit trail.
- Do not hardcode privileged credentials or role codes.

## Verification

```powershell
.\gradlew.bat testDebugUnitTest --tests "*Authorization*" --tests "*Session*"
.\gradlew.bat compileDebugKotlin
```

## Web Research Directive
**Last Verified Date:** 2026-06-08

If the `Last Verified Date` above is more than 30 days older than the current date, you MUST use the `search_web` tool to search the official documentation at **docs.gradle.org** or **developer.android.com** (Google for Developers) to ensure that the APIs, configurations, and best practices are up-to-date with the latest stable releases. Do not rely solely on your pre-training data. If you perform this verification, you MUST update the `Last Verified Date` in this file to the current date to prevent redundant web searches in future executions.

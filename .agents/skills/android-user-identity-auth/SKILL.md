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
Before executing any actions based on this skill, you MUST use the `search_web` tool to search the official documentation at **docs.gradle.org** or **developer.android.com** (Google for Developers). Ensure that the APIs, Gradle configurations, and best practices you are about to apply are completely up-to-date with the latest stable releases. Do not rely solely on your pre-training data.

---
name: android-testing-compose-room-workmanager
description: Use this skill for Bocatta-specific Android tests around Compose UI, Room/SQLite offline data, WorkManager sync, Koin wiring, Firebase fakes, and POS critical flows.
metadata:
  keywords:
  - android
  - testing
  - compose tests
  - room tests
  - workmanager tests
  - koin
---

# Android Testing For Bocatta

Use this when adding or repairing tests for POS flows, offline data, sync, Compose UI, or DI.

## Bocatta Context

Existing test areas:

- `app/src/test/java/com/bocatta/pos/domain/*`
- `app/src/test/java/com/bocatta/pos/data/queue/*`
- `app/src/test/java/com/bocatta/pos/di/KoinModuleTest.kt`
- `app/src/androidTest/java/com/bocatta/pos/presentation/ui/screens/ventas/SalesScreenTest.kt`
- `app/src/androidTest/java/com/bocatta/pos/data/local/OfflineDatabaseFolioInstrumentedTest.kt`
- `maestro/sale_flow.yaml`

## Workflow

1. Pick the smallest test type that proves the behavior: unit, instrumented, Compose UI, or Maestro.
2. For domain math and inventory deductions, prefer unit tests.
3. For Room/SQLite folios and offline persistence, use instrumented or in-memory database tests.
4. For WorkManager, test input/output state and queue transitions.
5. For Compose, test user-observable behavior instead of implementation details.
6. Use fakes before network calls; do not require live Firestore for normal tests.

## Guardrails

- Do not weaken existing critical inventory/offline assertions.
- Do not add sleeps when an idling/testable state is available.
- Do not make tests depend on real branches, employees, or production Firebase data.

## Verification

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
```

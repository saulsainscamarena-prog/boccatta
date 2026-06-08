---
name: android-room-offline-storage
description: Use this skill for Bocatta POS offline persistence with Room or SQLite, including OfflineDatabase, local sale folios, stock adjustment queues, migration safety, and durable offline checkout data.
metadata:
  keywords:
  - android
  - room
  - sqlite
  - offline
  - persistence
  - bocatta
---

# Android Room Offline Storage

Use this when changing local persistence, offline sales, local inventory records, or Room/SQLite access.

## Bocatta Context

Start with:

- `app/src/main/java/com/bocatta/pos/data/local/OfflineDatabase.kt`
- `app/src/main/java/com/bocatta/pos/data/sync/OfflineManager.kt`
- `app/src/main/java/com/bocatta/pos/data/queue/SQLiteStockAdjustmentQueue.kt`
- `app/src/androidTest/java/com/bocatta/pos/data/local/OfflineDatabaseFolioInstrumentedTest.kt`
- `app/src/test/java/com/bocatta/pos/data/queue/StockAdjustmentQueueStressTest.kt`

## Workflow

1. Map which local table or queue owns the durable state.
2. Preserve folio generation, sale auditability, and stock deduction history.
3. Keep database access off Composables; use repositories, managers, or ViewModels.
4. Make writes atomic when sale, folio, and stock state must move together.
5. If schema changes, add a migration or prove the existing helper recreates safely for the target data.
6. Validate offline-first behavior without assuming Firestore is reachable.

## Guardrails

- Do not delete local records before they are synced or explicitly marked terminal.
- Do not hide database errors behind generic `false` results without logging/reporting the cause.
- Do not duplicate local stock deduction logic already handled by `InventoryDeductions` or related repository methods.

## Verification

```powershell
.\gradlew.bat testDebugUnitTest --tests "*Offline*" --tests "*StockAdjustmentQueue*"
.\gradlew.bat compileDebugKotlin
```

## Web Research Directive
Before executing any actions based on this skill, you MUST use the `search_web` tool to search the official documentation at **docs.gradle.org** or **developer.android.com** (Google for Developers). Ensure that the APIs, Gradle configurations, and best practices you are about to apply are completely up-to-date with the latest stable releases. Do not rely solely on your pre-training data.

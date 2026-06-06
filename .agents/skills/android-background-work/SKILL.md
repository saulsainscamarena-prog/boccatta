---
name: android-background-work
description: Use this skill for Android background work in Bocatta POS, especially WorkManager, SyncWorker, offline sale sync, stock adjustment queues, retry policy, foreground/background restrictions, and battery-safe scheduled work.
metadata:
  keywords:
  - android
  - workmanager
  - background work
  - sync
  - offline
  - bocatta
---

# Android Background Work

Use this when touching background execution, synchronization, retry behavior, or any task that must continue after the app is backgrounded.

## Bocatta Context

Relevant files usually include:

- `app/src/main/java/com/bocatta/pos/data/sync/SyncWorker.kt`
- `app/src/main/java/com/bocatta/pos/data/sync/SyncScheduler.kt`
- `app/src/main/java/com/bocatta/pos/data/sync/OfflineManager.kt`
- `app/src/main/java/com/bocatta/pos/data/queue/SQLiteStockAdjustmentQueue.kt`
- `app/src/main/java/com/bocatta/pos/domain/repository/IStockAdjustmentQueue.kt`
- `app/src/main/java/com/bocatta/pos/data/repository/InventoryDeductions.kt`

## Workflow

1. Identify whether the task is immediate UI work, durable background work, or exact scheduled work.
2. Prefer existing WorkManager scheduling and queue abstractions before adding new schedulers.
3. Preserve offline sales and stock adjustments as auditable, retryable records.
4. Do not mark queued work complete until the remote Firestore operation and local state transition are both handled.
5. Make retry/failure states explicit; avoid silent catch blocks.
6. Check Android background restrictions before introducing services, alarms, or long-running work.

## Guardrails

- Do not describe the existing sync flow as two-phase commit unless the implementation truly guarantees that protocol.
- Do not bypass `IStockAdjustmentQueue` or `SyncErrorRepositoryImpl` for critical sync errors.
- Do not start direct foreground services unless WorkManager is insufficient and the user-facing need is clear.

## Verification

Run focused tests around sync and queues when changed:

```powershell
.\gradlew.bat testDebugUnitTest --tests "*Sync*" --tests "*StockAdjustment*"
.\gradlew.bat compileDebugKotlin
```

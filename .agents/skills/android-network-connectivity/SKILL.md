---
name: android-network-connectivity
description: Use this skill for Android network connectivity in Bocatta POS, including NetworkStateProvider, online/offline transitions, Firestore availability, sync triggers, and preserving checkout during outages.
metadata:
  keywords:
  - android
  - connectivity
  - network
  - offline
  - firestore
  - sync
---

# Android Network Connectivity

Use this when changing network detection, online/offline UX, Firestore retry behavior, or sync scheduling based on connectivity.

## Bocatta Context

Start with:

- `app/src/main/java/com/bocatta/pos/network/NetworkStateProvider.kt`
- `app/src/main/java/com/bocatta/pos/data/sync/OfflineManager.kt`
- `app/src/main/java/com/bocatta/pos/data/sync/SyncScheduler.kt`
- `app/src/main/java/com/bocatta/pos/network/firebase/FirebaseFirestoreProvider.kt`
- `app/src/main/AndroidManifest.xml`

## Workflow

1. Treat connectivity as a signal, not a guarantee that Firestore writes will succeed.
2. Preserve checkout behavior during outages.
3. Queue durable work before trying best-effort sync.
4. Make online/offline state visible where it affects operator decisions.
5. Avoid blocking UI while probing network state.
6. Recheck sync behavior after process restart.

## Guardrails

- Do not skip local offline save just because network appears available.
- Do not assume `ACCESS_NETWORK_STATE` means internet reachability.
- Do not create polling loops when WorkManager constraints or callbacks fit.

## Verification

```powershell
.\gradlew.bat testDebugUnitTest --tests "*Sync*" --tests "*Offline*"
.\gradlew.bat compileDebugKotlin
```

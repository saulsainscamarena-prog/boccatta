# Proposal

## Objective

Reduce debug/AVD startup blocking so the emulator can be used reliably for full mostrador validation.

## Non-Goals

- No sales, inventory, sync, checkout, ticket, auth, or UI redesign changes.
- No Gradle version or dependency version changes.
- No release telemetry removal.

## Proposed Changes

- Disable auto-starting log cleanup WorkManager scheduling in debug builds.
- Keep log pruning available, but run it only in non-debug startup or explicit future diagnostics.
- Add notes and verification evidence for the AVD startup condition.
- If needed, add debug manifest provider overrides only for telemetry providers that are not required for Firestore/Auth.

## Risks

- WorkManager is used for sync elsewhere; this change must only affect log cleanup startup scheduling, not `SyncWorker` scheduling from app flows.
- Removing Firebase providers globally would break Auth/Firestore, so provider changes must be debug-only and narrow.

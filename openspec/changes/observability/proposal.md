# Proposal: Observability Enhancements (Audit Only)

## Current State Evaluation
The observability strategy in Bocatta POS is robust and well-suited for a hybrid online/offline Point of Sale system. 
Key strengths include:
1. **Offline Resilience**: Local JSON-formatted file logging allows operators to pull diagnostic reports locally (`LogHelper.buildDiagnosticReport`).
2. **Release Safety**: Firebase Analytics and Crashlytics are neatly sandboxed into release builds via `releaseImplementation`, preventing debug/test environments from polluting production data or increasing build times.
3. **Structured Sync Diagnostics**: The `sync_errors` collection in Firestore neatly centralizes critical offline sync failures (e.g., `Max retries exceeded`, Idempotency issues) with associated stack traces and sale IDs.
4. **Maintenance automation**: `LogCleanupWorker` effectively manages disk usage by pruning logs older than 15 days.

## Recommended Enhancements (No Code Changes Yet)
As requested, this audit proposes the following potential future improvements without modifying code:

1. **Explicit Business Telemetry**:
   - Introduce specific analytics tracking for business-critical actions (e.g., `checkout_completed`, `contingency_shift_opened`, `sync_failed`) using Firebase Analytics, wrapping the API to maintain safety in debug environments.

2. **Breadcrumb Integration Improvement**:
   - Currently, `LogHelper.recordBreadcrumb` adds strings to an array and prints a log message. Consider sending critical business actions directly to Firebase Crashlytics as custom logs (via `FirebaseCrashlytics.getInstance().log()`), so they are automatically attached to all crash reports rather than just fatal exceptions.

3. **Storage Bounds for Local Logs**:
   - While `LogCleanupWorker` cleans up logs older than 15 days, high-frequency logging during extended offline periods could still result in large file sizes. Adding a maximum total directory size cap (e.g., 50 MB) would protect devices with limited storage.

4. **SyncError Monitoring Strategy**:
   - Currently, `SyncWorker` logs transient failures locally, but transient failures that reach `MAX_INTENTOS` are upgraded to critical. Relying purely on Firestore's `sync_errors` collection means admins need to check the database manually. Consider integrating Google Cloud alerts or Firebase Functions to notify administrators when documents are added to `sync_errors`.

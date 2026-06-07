# Research: Observability, Logging, and Crash Reporting in Bocatta POS

## Current Implementation

### 1. Timber & Logcat
- `Timber` is used throughout the application for logging.
- `LogHelper.kt` configures Timber trees during application initialization (`BocattaApp.kt`).
- Debug builds use `Timber.DebugTree()` for local logcat visibility.
- Release builds suppress standard logs (`VERBOSE`, `DEBUG`, `INFO`) and redirect `WARN`, `ERROR`, and `ASSERT` logs to Firebase Crashlytics via a custom `CrashlyticsTree`.

### 2. Local File Logging (`FileLoggingTree`)
- Captures all log events as JSON objects containing timestamp, log level, tag, message, and error stack trace (if any).
- These logs are stored locally in the device's external or internal `logs` directory (`app_<date>.log`).
- Ensures offline/on-device diagnostic capabilities even if sync fails.

### 3. Firebase Analytics & Crashlytics
- Configured in `build.gradle.kts` as release-only dependencies:
  ```kotlin
  releaseImplementation(libs.firebase.analytics)
  releaseImplementation(libs.firebase.crashlytics)
  ```
- `CrashlyticsTree` forwards higher severity logs to Crashlytics using reflection to safely handle the dependency absence in debug builds.
- There are no explicit Firebase Analytics events manually triggered in the codebase; it relies on default automatic event collection.

### 4. Crash Handling and Breadcrumbs
- `BocattaCrashHandler` intercepts uncaught exceptions.
- It writes a specific `crash_<timestamp>.log` file locally with diagnostic information (Thread, App version, Android version, Device, Breadcrumbs, and StackTrace).
- Maintains up to 40 "breadcrumbs" (e.g., `app_start` or custom events via `LogHelper.recordBreadcrumb`) in memory.
- Appends breadcrumbs as a custom key to Firebase Crashlytics before delegating to the default uncaught exception handler (which typically crashes the app).

### 5. Local Log Cleanup (`LogCleanupWorker`)
- Managed by WorkManager, executed periodically (daily).
- Keeps log files for a maximum of 15 days (`15L * 24 * 60 * 60 * 1000`).
- Deletes older log files to manage local disk space.

### 6. Sync Error Reporting (`SyncErrorRepositoryImpl`)
- Reports critical sync errors (e.g., failed stock adjustments, exceeded retries in `SyncWorker`, critical serialization/deserialization exceptions).
- Pushes `SyncError` models directly to the `sync_errors` collection in Firestore.
- Contains the device ID, timestamp, error message, failed document/sale IDs, app version, and stack trace.

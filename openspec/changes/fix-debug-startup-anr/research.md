# Research

## Scope

No external web research was required. Evidence came from AVD runtime traces, logcat, dropbox ANR data, local SQLite inspection, and the existing Bocatta POS codebase.

## Local Evidence

- AVD `bocatta_tablet_api36` initially became disconnected because of stale ADB state and mixed ADB binaries.
- After restarting ADB with `D:\Android\Sdk\platform-tools\adb.exe`, the app launched but `am start -W` timed out.
- Current launch eventually reached `Routes.Login` and later completed a sale, but ActivityTaskManager reported launch timeout and HWUI reported long frames.
- Historical dropbox ANRs show startup failures in:
  - `FirebaseInitProvider` eager initialization, including Crashlytics.
  - `BocattaApp.onCreate` while creating WorkManager `WorkSpec`.
- Current logcat still shows WorkManager force-stop reconciliation and Firebase provider/API initialization during startup.

## Constraints

- This phase must not change sales behavior, stock deductions, offline queue semantics, checkout, Firebase rules, or UI redesign.
- Any debug-only startup mitigation must preserve release Crashlytics/Analytics behavior.

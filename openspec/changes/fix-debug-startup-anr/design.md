# Design

## Entry Point

`BocattaApp.onCreate` is the startup owner for local logging and log cleanup scheduling.

## Decision

Move non-critical log cleanup WorkManager scheduling behind `!BuildConfig.DEBUG`. Debug AVD validation does not need daily log cleanup on every install/start, and WorkManager force-stop reconciliation was visible during startup.

## Why Not Disable WorkManager Globally

WorkManager is part of the offline/sync architecture. Disabling the initializer or dependency globally would risk sale sync and stock adjustment paths. This phase only removes a non-critical maintenance worker from debug startup.

## Verification

- Compile after code change.
- Assemble debug APK.
- Reinstall and launch on AVD.
- Capture `am start -W`, logcat breadcrumbs, and UI dump/screenshot.

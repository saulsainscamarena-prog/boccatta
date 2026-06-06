# Bocatta Android Skills

These local skills are available to Codex, Antigravity, OpenCode, or any agent working in this repository. When a task matches one of these areas, read the corresponding `.agents/skills/<skill-name>/SKILL.md` before changing code.

## High Priority

- `android-background-work`: WorkManager, `SyncWorker`, offline sync, stock adjustment queues.
- `android-room-offline-storage`: `OfflineDatabase`, Room/SQLite, offline sale folios, durable local checkout state.
- `android-data-files`: local files, logs, backups, exports, safe report/ticket data.
- `android-compose-performance`: sales grid/cart recomposition, `derivedStateOf`, Lazy layout performance.
- `android-compose-state-side-effects`: one-shot events, dialogs, snackbars, lifecycle-aware Compose state.
- `android-security-permissions`: manifest, permissions, Firebase rules impact, sensitive POS data.
- `android-user-identity-auth`: Firebase Auth, roles, sessions, employees, admin authorization.
- `android-app-architecture`: ViewModels, repositories, use cases, Koin, domain/data boundaries.
- `android-testing-compose-room-workmanager`: Compose, Room, WorkManager, Koin and POS critical-flow tests.
- `android-performance-baseline-profiles`: startup and release performance for counter workflows.

## Medium Priority

- `android-intents-sharing`: WhatsApp/generic sharing for tickets, payroll, reports.
- `android-network-connectivity`: online/offline transitions, `NetworkStateProvider`, Firestore availability.
- `android-app-compatibility-target-sdk`: SDK behavior changes, manifest restrictions, compatibility checks.
- `android-gradle-build-optimization`: wrapper, version catalog, KSP, R8, Detekt, Jacoco, Windows daemon issues.
- `android-crash-reporting-observability`: Timber, analytics, local logs, sync errors, diagnostics.
- `android-accessibility-compose`: touch targets, semantics, focus, contrast in Compose screens.
- `android-backup-data-extraction`: `backup_rules.xml`, `data_extraction_rules.xml`, offline data restore risk.
- `android-release-play-distribution`: App Bundles, release config, R8, Play/internal distribution checks.

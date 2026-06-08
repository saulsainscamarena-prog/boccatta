---
name: android-data-files
description: Use this skill for Android data and file handling in Bocatta POS, including local logs, exported reports, backups, JSON serialization, app-specific storage, and safe handling of POS-sensitive files.
metadata:
  keywords:
  - android
  - data files
  - storage
  - backup
  - export
  - logs
---

# Android Data And Files

Use this when handling local files, exports, backups, serialized JSON, or shareable POS data.

## Bocatta Context

Relevant areas:

- `app/src/main/java/com/bocatta/pos/logging/LogCleanupWorker.kt`
- `app/src/main/java/com/bocatta/pos/data/local/OfflineDatabase.kt`
- `app/src/main/java/com/bocatta/pos/domain/usecase/GenerarTicketWhatsAppUseCase.kt`
- `app/src/main/res/xml/backup_rules.xml`
- `app/src/main/res/xml/data_extraction_rules.xml`

## Workflow

1. Classify the data: operational, audit, report, ticket, cache, or sensitive.
2. Prefer app-specific storage for internal files.
3. Do not request broad storage permissions unless the core user-facing feature requires it.
4. Keep WhatsApp/report text plain and compatible with generic share targets.
5. Avoid storing credentials, employee secrets, or sensitive operational data in shareable files.
6. If a file should not be backed up, update backup/data extraction rules.

## Guardrails

- Do not use external public storage for offline sales or audit logs.
- Do not hardcode absolute user paths.
- Do not add `MANAGE_EXTERNAL_STORAGE` for ordinary exports.

## Verification

Check manifest permissions and run:

```powershell
.\gradlew.bat compileDebugKotlin
```

## Web Research Directive
Before executing any actions based on this skill, you MUST use the `search_web` tool to search the official documentation at **docs.gradle.org** or **developer.android.com** (Google for Developers). Ensure that the APIs, Gradle configurations, and best practices you are about to apply are completely up-to-date with the latest stable releases. Do not rely solely on your pre-training data.

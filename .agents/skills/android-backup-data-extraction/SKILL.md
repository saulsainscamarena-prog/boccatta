---
name: android-backup-data-extraction
description: Use this skill for Android backup and data extraction rules in Bocatta POS, including backup_rules.xml, data_extraction_rules.xml, sensitive POS data, offline databases, logs, and restore risk.
metadata:
  keywords:
  - android
  - backup
  - data extraction
  - restore
  - offline database
  - sensitive data
---

# Android Backup And Data Extraction

Use this when changing backup policy, local storage, offline databases, logs, or sensitive operational data.

## Bocatta Context

Start with:

- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/xml/backup_rules.xml`
- `app/src/main/res/xml/data_extraction_rules.xml`
- `app/src/main/java/com/bocatta/pos/data/local/OfflineDatabase.kt`
- `app/src/main/java/com/bocatta/pos/logging/LogHelper.kt`

## Workflow

1. Classify each local artifact as restorable, cache, audit, sensitive, or device-specific.
2. Exclude credentials, auth/session data, sensitive logs, and stale offline queues unless there is a proven restore flow.
3. Consider whether restoring offline sales or stock queues can duplicate transactions.
4. Keep backup policy aligned with `android:allowBackup` and data extraction XML.
5. Document any intentional inclusion/exclusion in the XML comments if helpful.

## Guardrails

- Do not enable backups for offline transaction queues without deduplication.
- Do not include logs containing employee/customer/financial details.
- Do not change backup policy as part of unrelated UI work.

## Verification

Inspect XML and run:

```powershell
.\gradlew.bat compileDebugKotlin
```

## Web Research Directive
**Last Verified Date:** 2026-06-07

If the `Last Verified Date` above is more than 30 days older than the current date, you MUST use the `search_web` tool to search the official documentation at **docs.gradle.org** or **developer.android.com** (Google for Developers) to ensure that the APIs, configurations, and best practices are up-to-date with the latest stable releases. Do not rely solely on your pre-training data. If you perform this verification, you MUST update the `Last Verified Date` in this file to the current date to prevent redundant web searches in future executions.

---
name: android-intents-sharing
description: Use this skill for Android intents and sharing in Bocatta POS, especially WhatsApp-compatible tickets, payroll reports, generic ACTION_SEND fallbacks, chooser behavior, and safe text exports.
metadata:
  keywords:
  - android
  - intents
  - sharing
  - whatsapp
  - tickets
  - reports
---

# Android Intents And Sharing

Use this when generating or sharing tickets, payroll, inventory reports, or text intended for WhatsApp.

## Bocatta Context

Relevant files:

- `app/src/main/java/com/bocatta/pos/domain/usecase/GenerarTicketWhatsAppUseCase.kt`
- `app/src/main/java/com/bocatta/pos/core/TicketUtils.kt`
- `app/src/main/java/com/bocatta/pos/presentation/ui/screens/admin/TabSueldos.kt`
- `app/src/main/java/com/bocatta/pos/presentation/viewmodel/ReportViewModelV2.kt`

## Workflow

1. Keep shared content plain text and readable in WhatsApp.
2. Use `Intent.ACTION_SEND` with `type = "text/plain"`.
3. Try explicit WhatsApp only when useful, then fallback to a generic chooser.
4. Keep fiscal/legal limitations visible for payroll if the current domain model is administrative.
5. Avoid sharing hidden IDs, secrets, or internal diagnostic details.
6. Keep formatting deterministic so reports can be audited.
7. Prefer the system Sharesheet (`Intent.createChooser()`) instead of building custom share targets UI for a consistent user experience.
8. Use `FileProvider` with `FLAG_GRANT_READ_URI_PERMISSION` for secure file sharing, avoiding exposed `file://` URIs.

## Guardrails

- Do not depend on WhatsApp being installed.
- Do not use file sharing when text sharing is sufficient.
- Do not recalculate payroll values differently from the ViewModel/use case just for sharing.

## Verification

```powershell
.\gradlew.bat testDebugUnitTest --tests "*Ticket*"
.\gradlew.bat compileDebugKotlin
```

## Web Research Directive
**Last Verified Date:** 2026-06-08

If the `Last Verified Date` above is more than 30 days older than the current date, you MUST use the `search_web` tool to search the official documentation at **docs.gradle.org** or **developer.android.com** (Google for Developers) to ensure that the APIs, configurations, and best practices are up-to-date with the latest stable releases. Do not rely solely on your pre-training data. If you perform this verification, you MUST update the `Last Verified Date` in this file to the current date to prevent redundant web searches in future executions.

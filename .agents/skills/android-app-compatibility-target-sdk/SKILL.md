---
name: android-app-compatibility-target-sdk
description: Use this skill for Android app compatibility and target SDK changes in Bocatta POS, including compileSdk/targetSdk behavior, manifest restrictions, backup changes, background limits, permissions, and platform behavior changes.
metadata:
  keywords:
  - android
  - compatibility
  - target sdk
  - compile sdk
  - platform changes
  - manifest
---

# Android App Compatibility And Target SDK

Use this when changing compile SDK, target SDK, manifest behavior, permissions, backup policy, or platform-sensitive APIs.

## Bocatta Context

The project currently uses:

- `compileSdk = 36`
- `targetSdk = 36`
- `minSdk = 24`
- Android Gradle Plugin from `gradle/libs.versions.toml`
- JDK 17 via Gradle/Kotlin configuration

## Workflow

1. Check `app/build.gradle.kts`, `gradle/libs.versions.toml`, and `gradle/gradle-daemon-jvm.properties`.
2. Review platform behavior changes for the target SDK being used.
3. Check manifest components, backup rules, permissions, background work, and notification/foreground-service behavior.
4. Keep JDK alignment at 17 unless an explicit migration is requested and verified.
5. Avoid SDK or AGP changes unless the task requires them.

## Guardrails

- Do not diagnose Gradle alias/version catalog issues before checking local daemon/lock problems described in `AGENTS.md`.
- Do not change target SDK casually; it can alter runtime behavior.
- Do not use global Gradle.

## Verification

```powershell
.\gradlew.bat --version
.\gradlew.bat compileDebugKotlin
```

## Web Research Directive
Before executing any actions based on this skill, you MUST use the `search_web` tool to search the official documentation at **docs.gradle.org** or **developer.android.com** (Google for Developers). Ensure that the APIs, Gradle configurations, and best practices you are about to apply are completely up-to-date with the latest stable releases. Do not rely solely on your pre-training data.

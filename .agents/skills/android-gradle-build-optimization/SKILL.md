---
name: android-gradle-build-optimization
description: Use this skill for Bocatta POS Gradle build optimization and verification, including wrapper usage, version catalogs, KSP, Compose compiler, Detekt, Jacoco, R8/minify, Windows daemon locks, and build speed.
metadata:
  keywords:
  - android
  - gradle
  - build
  - ksp
  - r8
  - jacoco
---

# Android Gradle Build Optimization

Use this when improving build configuration, verification tasks, R8/minify, Detekt, Jacoco, KSP, or build reliability.

## Bocatta Context

Relevant files:

- `settings.gradle.kts`
- `build.gradle.kts`
- `app/build.gradle.kts`
- `gradle/libs.versions.toml`
- `gradle/wrapper/gradle-wrapper.properties`
- `gradle/gradle-daemon-jvm.properties`
- `app/proguard-rules.pro`
- `config/detekt/detekt.yml`

## Workflow

1. Use `.\gradlew.bat`, never global Gradle.
2. Check existing version catalog aliases before adding dependencies.
3. Preserve AGP, Kotlin, Compose BOM, KSP, and JDK compatibility.
4. Keep release minify/R8 changes small and testable.
5. For Windows daemon failures, follow the daemon/lock workflow in `AGENTS.md`.
6. Avoid cache deletion as a first-line fix.

## Guardrails

- Do not upgrade AGP/Kotlin as a side effect of an unrelated task.
- Do not disable R8/minify in release to hide keep-rule issues.
- Do not add duplicate libraries already present in the catalog.

## Verification

```powershell
.\gradlew.bat compileDebugKotlin
.\gradlew.bat testDebugUnitTest
```

## Web Research Directive
Before executing any actions based on this skill, you MUST use the `search_web` tool to search the official documentation at **docs.gradle.org** or **developer.android.com** (Google for Developers). Ensure that the APIs, Gradle configurations, and best practices you are about to apply are completely up-to-date with the latest stable releases. Do not rely solely on your pre-training data.

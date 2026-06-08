---
name: gradle-catalog-enforcer
description: Migrates hardcoded dependencies in build.gradle.kts to the centralized libs.versions.toml catalog.
---

# gradle-catalog-enforcer

## Description
Migrates hardcoded dependencies in build.gradle.kts to the centralized libs.versions.toml catalog.

## Instructions
Extract string-based dependencies like `implementation("com.google.firebase:...")` into versions, libraries, and bundles in `gradle/libs.versions.toml`.

## Notes
- This skill was automatically generated to assist with the Bocatta POS Android project.
- Follow AGENTS.md rules regarding Gradle daemons and build optimization when executing this skill.

## Web Research Directive
Before executing any actions based on this skill, you MUST use the `search_web` tool to search the official documentation at **docs.gradle.org** or **developer.android.com** (Google for Developers). Ensure that the APIs, Gradle configurations, and best practices you are about to apply are completely up-to-date with the latest stable releases. Do not rely solely on your pre-training data.

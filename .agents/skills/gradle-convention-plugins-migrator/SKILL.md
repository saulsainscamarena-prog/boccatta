---
name: gradle-convention-plugins-migrator
description: Extracts repetitive build logic into a build-logic module with Kotlin DSL Convention Plugins.
---

# gradle-convention-plugins-migrator

## Description
Extracts repetitive build logic into a build-logic module with Kotlin DSL Convention Plugins.

## Instructions
Create `build-logic/convention` with precompiled script plugins (e.g. `bocatta.android.application.gradle.kts`) to share setup across modules.

## Notes
- This skill was automatically generated to assist with the Bocatta POS Android project.
- Follow AGENTS.md rules regarding Gradle daemons and build optimization when executing this skill.

## Web Research Directive
Before executing any actions based on this skill, you MUST use the `search_web` tool to search the official documentation at **docs.gradle.org** or **developer.android.com** (Google for Developers). Ensure that the APIs, Gradle configurations, and best practices you are about to apply are completely up-to-date with the latest stable releases. Do not rely solely on your pre-training data.

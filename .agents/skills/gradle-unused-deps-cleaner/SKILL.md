---
name: gradle-unused-deps-cleaner
description: Finds and removes unused dependencies to reduce APK size using dependency-analysis plugins.
---

# gradle-unused-deps-cleaner

## Description
Finds and removes unused dependencies to reduce APK size using dependency-analysis plugins.

## Instructions
Apply a dependency analysis plugin (e.g., com.autonomousapps.dependency-analysis) and follow its advice to remove unused `api` or `implementation` declarations.

## Notes
- This skill was automatically generated to assist with the Bocatta POS Android project.
- Follow AGENTS.md rules regarding Gradle daemons and build optimization when executing this skill.

## Web Research Directive
Before executing any actions based on this skill, you MUST use the `search_web` tool to search the official documentation at **docs.gradle.org** or **developer.android.com** (Google for Developers). Ensure that the APIs, Gradle configurations, and best practices you are about to apply are completely up-to-date with the latest stable releases. Do not rely solely on your pre-training data.

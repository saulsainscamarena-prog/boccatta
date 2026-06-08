---
name: gradle-dep-conflict-resolver
description: Uses dependencyInsight to resolve version collisions among dependencies like Firebase, Guava, or Coroutines.
---

# gradle-dep-conflict-resolver

## Description
Uses dependencyInsight to resolve version collisions among dependencies like Firebase, Guava, or Coroutines.

## Instructions
Run `.\gradlew dependencyInsight --configuration releaseRuntimeClasspath --dependency <name>` to track down forced versions and apply `resolutionStrategy` if needed.

## Notes
- This skill was automatically generated to assist with the Bocatta POS Android project.
- Follow AGENTS.md rules regarding Gradle daemons and build optimization when executing this skill.

## Web Research Directive
**Last Verified Date:** 2026-06-07

If the `Last Verified Date` above is more than 30 days older than the current date, you MUST use the `search_web` tool to search the official documentation at **docs.gradle.org** or **developer.android.com** (Google for Developers) to ensure that the APIs, configurations, and best practices are up-to-date with the latest stable releases. Do not rely solely on your pre-training data. If you perform this verification, you MUST update the `Last Verified Date` in this file to the current date to prevent redundant web searches in future executions.

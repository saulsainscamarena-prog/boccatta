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
Before executing any actions based on this skill, you MUST use the `search_web` tool to search the official documentation at **docs.gradle.org** or **developer.android.com** (Google for Developers). Ensure that the APIs, Gradle configurations, and best practices you are about to apply are completely up-to-date with the latest stable releases. Do not rely solely on your pre-training data.

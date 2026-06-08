---
name: gradle-test-retry-configurator
description: Configures the Test Retry plugin to automatically re-run flaky UI tests on Android emulators/Firebase Test Lab.
---

# gradle-test-retry-configurator

## Description
Configures the Test Retry plugin to automatically re-run flaky UI tests on Android emulators/Firebase Test Lab.

## Instructions
Apply the `org.gradle.test-retry` plugin and configure `maxRetries` for `androidTest` to handle emulator lag.

## Notes
- This skill was automatically generated to assist with the Bocatta POS Android project.
- Follow AGENTS.md rules regarding Gradle daemons and build optimization when executing this skill.

## Web Research Directive
Before executing any actions based on this skill, you MUST use the `search_web` tool to search the official documentation at **docs.gradle.org** or **developer.android.com** (Google for Developers). Ensure that the APIs, Gradle configurations, and best practices you are about to apply are completely up-to-date with the latest stable releases. Do not rely solely on your pre-training data.

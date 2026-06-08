---
name: gradle-config-cache-enabler
description: Enables and tests Gradle configuration cache, fixing incompatible tasks to reduce configuration time.
---

# gradle-config-cache-enabler

## Description
Enables and tests Gradle configuration cache, fixing incompatible tasks to reduce configuration time.

## Instructions
Run builds with `--configuration-cache`. Identify tasks that break configuration cache (like file I/O during configuration phase) and refactor them to use Providers/lazy evaluation.

## Notes
- This skill was automatically generated to assist with the Bocatta POS Android project.
- Follow AGENTS.md rules regarding Gradle daemons and build optimization when executing this skill.

## Web Research Directive
Before executing any actions based on this skill, you MUST use the `search_web` tool to search the official documentation at **docs.gradle.org** or **developer.android.com** (Google for Developers). Ensure that the APIs, Gradle configurations, and best practices you are about to apply are completely up-to-date with the latest stable releases. Do not rely solely on your pre-training data.

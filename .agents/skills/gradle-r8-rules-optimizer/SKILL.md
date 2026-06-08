---
name: gradle-r8-rules-optimizer
description: Analyzes mapping.txt and usage.txt to optimize ProGuard rules and fix reflection crashes in release builds.
---

# gradle-r8-rules-optimizer

## Description
Analyzes mapping.txt and usage.txt to optimize ProGuard rules and fix reflection crashes in release builds.

## Instructions
Run `assembleRelease` and inspect R8 output. Add precise `-keep` rules for Room entities and Firebase models instead of wildcarding entire packages.

## Notes
- This skill was automatically generated to assist with the Bocatta POS Android project.
- Follow AGENTS.md rules regarding Gradle daemons and build optimization when executing this skill.

## Web Research Directive
Before executing any actions based on this skill, you MUST use the `search_web` tool to search the official documentation at **docs.gradle.org** or **developer.android.com** (Google for Developers). Ensure that the APIs, Gradle configurations, and best practices you are about to apply are completely up-to-date with the latest stable releases. Do not rely solely on your pre-training data.

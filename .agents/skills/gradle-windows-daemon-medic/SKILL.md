---
name: gradle-windows-daemon-medic
description: Detect and kill rogue Kotlin/Gradle daemons on Windows, clean .lck files safely, and run --stop.
---

# gradle-windows-daemon-medic

## Description
Detect and kill rogue Kotlin/Gradle daemons on Windows, clean .lck files safely, and run --stop.

## Instructions
Use this skill when you encounter java.nio.file.AccessDeniedException or zip.lck errors on Windows. Run `.\gradlew --status` and `.\gradlew --stop`, and forcefully kill lingering java.exe processes locking build files.

## Notes
- This skill was automatically generated to assist with the Bocatta POS Android project.
- Follow AGENTS.md rules regarding Gradle daemons and build optimization when executing this skill.

## Web Research Directive
**Last Verified Date:** 2026-06-07

If the `Last Verified Date` above is more than 30 days older than the current date, you MUST use the `search_web` tool to search the official documentation at **docs.gradle.org** or **developer.android.com** (Google for Developers) to ensure that the APIs, configurations, and best practices are up-to-date with the latest stable releases. Do not rely solely on your pre-training data. If you perform this verification, you MUST update the `Last Verified Date` in this file to the current date to prevent redundant web searches in future executions.

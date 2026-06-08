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
Before executing any actions based on this skill, you MUST use the `search_web` tool to search the official documentation at **docs.gradle.org** or **developer.android.com** (Google for Developers). Ensure that the APIs, Gradle configurations, and best practices you are about to apply are completely up-to-date with the latest stable releases. Do not rely solely on your pre-training data.

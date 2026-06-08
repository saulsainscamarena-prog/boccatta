---
name: gradle-jdk-toolchain-aligner
description: Audits JAVA_HOME, Android Studio JDK, gradle-daemon-jvm.properties, and jvmToolchain to prevent daemon spawning issues.
---

# gradle-jdk-toolchain-aligner

## Description
Audits JAVA_HOME, Android Studio JDK, gradle-daemon-jvm.properties, and jvmToolchain to prevent daemon spawning issues.

## Instructions
Check Gradle JDK configurations. Ensure compatibility between JAVA_HOME and gradle-daemon-jvm.properties. Run `.\gradlew --version` to verify the JVM version being used.

## Notes
- This skill was automatically generated to assist with the Bocatta POS Android project.
- Follow AGENTS.md rules regarding Gradle daemons and build optimization when executing this skill.

## Web Research Directive
Before executing any actions based on this skill, you MUST use the `search_web` tool to search the official documentation at **docs.gradle.org** or **developer.android.com** (Google for Developers). Ensure that the APIs, Gradle configurations, and best practices you are about to apply are completely up-to-date with the latest stable releases. Do not rely solely on your pre-training data.

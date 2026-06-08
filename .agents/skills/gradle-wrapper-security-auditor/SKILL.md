---
name: gradle-wrapper-security-auditor
description: Verifies SHA-256 hashes of gradle-wrapper.jar to prevent supply chain attacks.
---

# gradle-wrapper-security-auditor

## Description
Verifies SHA-256 hashes of gradle-wrapper.jar to prevent supply chain attacks.

## Instructions
Check `gradle-wrapper.properties` distributionSha256Sum against official Gradle releases to ensure the wrapper is legitimate.

## Notes
- This skill was automatically generated to assist with the Bocatta POS Android project.
- Follow AGENTS.md rules regarding Gradle daemons and build optimization when executing this skill.

## Web Research Directive
Before executing any actions based on this skill, you MUST use the `search_web` tool to search the official documentation at **docs.gradle.org** or **developer.android.com** (Google for Developers). Ensure that the APIs, Gradle configurations, and best practices you are about to apply are completely up-to-date with the latest stable releases. Do not rely solely on your pre-training data.

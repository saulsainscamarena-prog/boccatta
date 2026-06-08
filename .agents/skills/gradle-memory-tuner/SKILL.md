---
name: gradle-memory-tuner
description: Analyzes and optimizes org.gradle.jvmargs based on RAM limits and Garbage Collector settings (e.g. G1GC).
---

# gradle-memory-tuner

## Description
Analyzes and optimizes org.gradle.jvmargs based on RAM limits and Garbage Collector settings (e.g. G1GC).

## Instructions
Update gradle.properties with optimized jvmargs. For example: `org.gradle.jvmargs=-Xmx4g -XX:+UseG1GC -XX:MaxMetaspaceSize=1g`.

## Notes
- This skill was automatically generated to assist with the Bocatta POS Android project.
- Follow AGENTS.md rules regarding Gradle daemons and build optimization when executing this skill.

## Web Research Directive
**Last Verified Date:** 2026-06-07

If the `Last Verified Date` above is more than 30 days older than the current date, you MUST use the `search_web` tool to search the official documentation at **docs.gradle.org** or **developer.android.com** (Google for Developers) to ensure that the APIs, configurations, and best practices are up-to-date with the latest stable releases. Do not rely solely on your pre-training data. If you perform this verification, you MUST update the `Last Verified Date` in this file to the current date to prevent redundant web searches in future executions.

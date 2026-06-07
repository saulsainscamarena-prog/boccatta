# Spec

## Scenario: Debug app reaches login without startup ANR

Given a clean AVD session with `com.bocatta.pos` installed
When the app is launched from ADB
Then the app should reach Login or the restored route without a startup ANR
And startup work should not synchronously schedule non-critical log cleanup on the main thread.

## Scenario: Release observability remains available

Given a release build
When the application starts
Then file logging and Crashlytics/Analytics release dependencies remain available
And log cleanup remains scheduled.

## Scenario: Sync workers are not disabled

Given a sale or stock operation schedules sync
When the app is debug or release
Then existing sync scheduling paths must still be callable
And this change must not disable WorkManager globally.

# Proposal: Extract Hardcoded Strings

## 1. Problem Statement
The application contains numerous hardcoded strings within Jetpack Compose `.kt` files, specifically within `Text("...")` components. This prevents the application from being properly localized (i18n) and makes string management difficult.

## 2. Proposed Solution
Extract all hardcoded strings from `Text("...")` calls into the `res/values/strings.xml` file. A Python script will be created to automate the identification and extraction of these strings to minimize manual effort and reduce the risk of errors. The script will generate string resource IDs and the corresponding XML entries, and update the Compose files to use `stringResource(R.string.id)`.

## 3. Scope
- **In Scope:** 
  - Identification of hardcoded strings in `.kt` files inside `Text()` composables.
  - Creation of a Python script to automate the extraction.
  - Addition of extracted strings to `res/values/strings.xml`.
  - Updating `.kt` files to use `stringResource(...)`.
- **Out of Scope:** Translating strings to other languages (only extraction for now), extracting strings not used in `Text()`.

## 4. Alternative Approaches
- **Manual Extraction:** Developer manually finds and replaces each string using Android Studio's extraction feature. This is error-prone and time-consuming for 500+ strings.
- **Using Android Studio Lint/Inspections:** Running an inspection to find all hardcoded text and applying quick fixes in bulk. Sometimes this can be unstable for 500+ files or complex Compose layouts, but is a viable alternative if the script fails.

## 5. Decision
Proceed with a Python script for precise control and auditing of the extracted strings before applying the changes to the codebase.

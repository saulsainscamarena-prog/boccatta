# Design: Extract Hardcoded Strings

## 1. Component Architecture
- A standalone Python script `extract_strings.py` located in the `scratch` directory.

## 2. Technical Decisions
- Python is chosen for its strong regex capabilities and ease of scripting file modifications.
- The script will use a two-pass approach:
  - **Pass 1:** Scan all `.kt` files, extract hardcoded strings, generate unique IDs, and build a dictionary of string values to IDs.
  - **Pass 2:** Read existing `strings.xml`, append new strings, and then modify `.kt` files to replace hardcoded strings with `stringResource` and insert necessary imports.
- Regex pattern for extraction: Look for `Text(` followed by optional parameters, then a string literal `"[^"]+"`. Need to be careful with string interpolation (e.g., `Text("Total: $total")`), which might be better left alone or extracted carefully.

## 3. Dependencies
- Only standard Python libraries (`os`, `re`, `xml.etree.ElementTree`).

## 4. Security & Privacy
- N/A.

## 5. Performance
- Script execution time should be negligible for the size of the project.

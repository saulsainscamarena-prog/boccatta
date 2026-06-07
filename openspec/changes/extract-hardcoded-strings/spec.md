# Specification: Extract Hardcoded Strings

## 1. Requirements
- The script must identify `Text("some string")` patterns in `.kt` files.
- The script must ignore strings that are already using `stringResource()`.
- The script must generate a valid string resource ID from the string content (e.g., lowercase, replace spaces with underscores, strip special characters).
- The script must handle duplicate string values by reusing the same string resource ID.
- The script must append the new `<string name="...">...</string>` entries to `res/values/strings.xml`.
- The script must replace the hardcoded string in the `.kt` file with `stringResource(R.string.id)`.
- The script must add `import androidx.compose.ui.res.stringResource` and `import com.bocatta.pos.R` (or appropriate R package) to the `.kt` file if not present.

## 2. API / Interface Changes
- No API changes. This is a refactoring task.

## 3. Data Model Changes
- No database changes.

## 4. Constraints
- The script must not break existing Compose formatting.
- Do not run the script on `.kt` files until the user reviews it.

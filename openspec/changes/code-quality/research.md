# Code Quality Research

## Objective
Audit the Bocatta POS codebase for code quality, best practices, and code smells, utilizing Detekt, Android Lint, and textual searches for `TODO`s, `FIXME`s, and commented-out code.

## Findings

### 1. TODOs and FIXMEs
A comprehensive search was performed across the codebase using regex to find `TODO` and `FIXME` comments.

- **TODOs:** 1 found
  - `app/src/main/java/com/bocatta/pos/presentation/ui/screens/onboarding/OnboardingScreen.kt` (Line 81): `// TODO: Call Repository here`
- **FIXMEs:** 0 found

### 2. Commented-out Code
Both regex searches (`// var`, `// fun`, `// val`, etc.) and Detekt's analysis yielded **0** instances of obvious commented-out blocks of code that need removal. The codebase appears relatively clean of dead commented code.

### 3. Detekt Analysis
Detekt was executed (`.\gradlew.bat detekt`) to analyze Kotlin code smells and complexity. The task failed because it exceeded the `maxIssues` threshold (100).

- **Total Smells Found:** 280
- **Total Debt:** 23h 20min
- **Rule Violations:** 
  - `style, WildcardImport`: 280 issues. 
  - **Details:** The codebase relies heavily on wildcard imports (e.g., `import com.bocatta.pos.domain.model.*`, `import androidx.compose.runtime.*`, `import androidx.compose.foundation.layout.*`). Wildcard imports can lead to naming conflicts and make the codebase more brittle during library updates.
- **Complexity Metrics:**
  - **Total Lines of Code:** 41,202 (27,229 logical lines)
  - **Cyclomatic Complexity:** 4,539 (166 per 1,000 logical lines)
  - **Cognitive Complexity:** 5,434
  - **Comment Source Ratio:** 2% (986 comment lines vs 34,954 source lines)

### 4. Android Lint Analysis
Android Lint was successfully parsed from the existing `lint-results-debug.xml` report. A total of 100 Warnings were found, with 0 Errors.

- **Total Issues:** 100 (All Warnings)
- **Top Issue Types:**
  - `UnusedResources`: 41 instances (Dead string, drawable, or color resources).
  - `AutoboxingStateCreation`: 27 instances (Compose performance issue when using `mutableStateOf` with primitives).
  - `NewerVersionAvailable`: 7 instances (Dependencies have newer versions).
  - `GradleDependency`: 7 instances (Deprecated or misconfigured dependency versions).
  - `PluralsCandidate`: 4 instances (Hardcoded plural handling instead of using `res/values/plurals.xml`).
  - `ModifierParameter`: 4 instances (Compose function `Modifier` parameter not adhering to guidelines).
  - `DefaultLocale`: 4 instances (Using `String.format` or `.uppercase()` without explicit Locale).
  - `IconLocation`, `DataExtractionRules`, `RedundantLabel`, `OldTargetApi`, `Typos`, `DiscouragedApi`: 1 instance each.

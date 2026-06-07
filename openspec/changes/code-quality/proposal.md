# Proposal: Code Quality Enhancements

## Motivation
Following an audit of the Bocatta POS codebase, several code smells, lint warnings, and unresolved comments were identified. Resolving these issues will improve the overall performance, maintainability, and compilation health of the project, lowering tech debt. 

## Proposed Changes

1. **Resolve Detekt Code Smells (Wildcard Imports)**
   - **Issue:** 280 instances of wildcard imports (`.*`) exist.
   - **Solution:** Expand all wildcard imports to explicit fully qualified class imports to avoid naming collisions and to pass the Detekt validation.

2. **Address Android Lint Performance Warnings**
   - **Issue:** 27 `AutoboxingStateCreation` warnings in Compose.
   - **Solution:** Replace `mutableStateOf()` for primitives with `mutableIntStateOf()`, `mutableLongStateOf()`, `mutableFloatStateOf()`, or `mutableDoubleStateOf()` to avoid unnecessary autoboxing and allocation overhead during recomposition.

3. **Clean Up Unused Resources**
   - **Issue:** 41 `UnusedResources` warnings.
   - **Solution:** Remove all dead strings, drawables, and color resources that are no longer referenced in the application to optimize APK size.

4. **Fix Compose API Inconsistencies**
   - **Issue:** 4 `ModifierParameter` warnings.
   - **Solution:** Ensure all Composable functions that accept a `Modifier` place it as the first optional parameter with a default value of `Modifier`.

5. **Fix Formatting and Locale Warnings**
   - **Issue:** 4 `DefaultLocale` warnings.
   - **Solution:** Explicitly pass a `Locale` object (such as `Locale.getDefault()`) whenever invoking `.uppercase()`, `.lowercase()`, or `String.format()` to prevent unexpected behavior in different device languages.

6. **Address Pending TODOs**
   - **Issue:** 1 `TODO` at `OnboardingScreen.kt:81` (`// TODO: Call Repository here`).
   - **Solution:** Evaluate the onboarding logic and implement the required repository call, or remove the comment if the onboarding workflow is already fully functional.

7. **Coordinate Dependency Updates (Optional)**
   - **Issue:** 14 warnings regarding older library versions (`NewerVersionAvailable` and `GradleDependency`).
   - **Solution:** Coordinate dependency updates carefully to ensure they do not break compatibility with the Bocatta Windows Port. Only update versions if they address critical bugs.

## Architectural Integrity
None of these changes will introduce new architectural patterns. They are purely corrective to align with existing conventions, Jetpack Compose best practices, and Kotlin styling rules.

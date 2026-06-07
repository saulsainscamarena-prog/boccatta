# Execution Plan: Code Quality Enhancements

## Scope
This plan details the step-by-step process for resolving the code quality issues identified in the codebase without introducing new features or modifying the underlying application architecture. 

**Note:** As per the current mandate, no code modifications are to be executed directly in this phase. This plan serves as the blueprint for future execution.

## Tasks

### Phase 1: Lint Warnings (Compose and Resources)
1. **Optimize Compose State (AutoboxingStateCreation):**
   - Search the codebase for `mutableStateOf(0)`, `mutableStateOf(0f)`, `mutableStateOf(0.0)`.
   - Replace with `mutableIntStateOf(0)`, `mutableFloatStateOf(0f)`, `mutableDoubleStateOf(0.0)`.
2. **Standardize Modifiers (ModifierParameter):**
   - Locate the 4 Composable functions with incorrect modifier parameters.
   - Reorder parameters so that `modifier: Modifier = Modifier` is the first optional parameter.
3. **Remove Dead Resources (UnusedResources):**
   - Review the Android Lint report to identify the exact 41 unused resources.
   - Delete unused strings from `res/values/strings.xml`.
   - Delete unused drawables and vector icons.

### Phase 2: Detekt Violations (Imports and Formatting)
1. **Fix Wildcard Imports:**
   - Go through the 280 files flagged by Detekt.
   - Manually expand `import com.bocatta.pos.*` and `import androidx.compose.*` statements into explicit imports.
   - Remove redundant or unused imports along the way.
2. **Enforce Explicit Locales:**
   - Update the 4 instances of `.uppercase()`, `.lowercase()`, and `String.format()` to use `Locale.ROOT` or `Locale.getDefault()`.

### Phase 3: Pending Code (TODOs)
1. **Resolve `OnboardingScreen` TODO:**
   - Analyze line 81 in `app/src/main/java/com/bocatta/pos/presentation/ui/screens/onboarding/OnboardingScreen.kt`.
   - Determine the correct repository injection needed or remove the comment if the data layer is already wired correctly via ViewModel.

### Phase 4: Verification
1. **Re-run Detekt:**
   - Execute `.\gradlew.bat detekt --no-parallel` to ensure 0 weighted issues remain and the task passes successfully.
2. **Re-run Android Lint:**
   - Execute `.\gradlew.bat lintDebug --no-parallel` to verify the total warning count drops significantly.
3. **Ensure Compatibility:**
   - Compile the app using `.\gradlew.bat compileDebugKotlin` to verify no compilation errors were introduced by the import replacements.

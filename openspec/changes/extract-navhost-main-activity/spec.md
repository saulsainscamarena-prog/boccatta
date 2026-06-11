# Delta for Navigation Extraction

## ADDED Requirements

### Requirement: R1 – Create `AppNavGraph.kt`

The system **MUST** create a new file `AppNavGraph.kt` in the package `com.bocatta.pos.navigation` containing the `AppNavGraph` composable that encapsulates the existing NavHost block.

#### Scenario: File creation
- GIVEN the project structure
- WHEN the change is applied
- THEN `AppNavGraph.kt` exists under `app/src/main/java/com/bocatta/pos/navigation/`

### Requirement: R2 – Parameterize `AppNavGraph`

The `AppNavGraph` composable **MUST** receive the current authentication state and all navigation callback lambdas as parameters, matching the signatures used in `MainActivity`.

#### Scenario: Parameter passing
- GIVEN `MainActivity` has auth state and lambda callbacks
- WHEN `AppNavGraph` is invoked
- THEN all required parameters are supplied without alteration

## MODIFIED Requirements

### Requirement: R3 – Preserve route behavior

The system **MUST** render the exact same composable for each of the 19 routes with the exact same callbacks as before extraction. *(Previously: NavHost defined inline in `MainActivity`.)*

#### Scenario: Route rendering
- GIVEN any of the 19 routes is navigated to
- WHEN the NavHost is executed via `AppNavGraph`
- THEN the same destination composable and callbacks are invoked as in the original implementation

### Requirement: R4 – Conditional start destination

The navigation start destination **MUST** depend on authentication state exactly as before: logged‑in users start at `TurnosScreen`, logged‑out users start at `LoginScreen`. *(Previously: conditional `startDestination` in inline NavHost.)*

#### Scenario: Start destination when logged in
- GIVEN the user is authenticated
- WHEN the app launches
- THEN the NavHost starts at `TurnosScreen`

#### Scenario: Start destination when logged out
- GIVEN the user is not authenticated
- WHEN the app launches
- THEN the NavHost starts at `LoginScreen`

### Requirement: R5 – Call `AppNavGraph` from `MainActivity`

`MainActivity` **SHALL** replace the inline NavHost with a single call to `AppNavGraph(...)`, passing the required parameters. *(Previously: NavHost defined directly inside `MainActivity`.)*

#### Scenario: Invocation replacement
- GIVEN the original NavHost block is removed
- WHEN `MainActivity` compiles
- THEN it contains a call to `AppNavGraph` with matching arguments

### Requirement: R6 – Clean up imports

`MainActivity` **MUST** remove any imports that are no longer needed after extracting the NavHost. *(Previously: Navigation‑related imports remained.)*

#### Scenario: Import removal
- GIVEN the NavHost has been extracted
- WHEN the project is compiled
- THEN `MainActivity.kt` contains only the imports required for its remaining logic

### Requirement: R7 – Build and test integrity

The project **MUST** compile without errors and all existing tests **MUST** pass after the change. *(Previously: Build succeeded with inline NavHost.)*

#### Scenario: Successful build
- GIVEN the codebase with the extracted NavHost
- WHEN `./gradlew assembleDebug` is executed
- THEN the build succeeds

#### Scenario: Test suite passes
- GIVEN the updated project
- WHEN the test suite runs
- THEN all tests report success

## NON‑REQUIREMENTS

- No restructuring of the `Routes` sealed class.
- No modification of route arguments or deep‑link handling.
- No architectural changes beyond extracting the NavHost into its own composable.

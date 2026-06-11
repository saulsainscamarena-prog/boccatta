# Proposal: Extract NavHost from MainActivity

## Intent

The `MainActivity.kt` file has grown significantly, with approximately 250 lines dedicated solely to navigation logic. This bloat makes the file harder to maintain and violates the single responsibility principle. The goal is to extract the `NavHost` and its route definitions into a dedicated composable to improve codebase organization and maintainability.

## Scope

### In Scope
- Create `app/src/main/java/com/bocatta/pos/navigation/AppNavGraph.kt` containing the `AppNavGraph` composable.
- Move the `NavHost` block and all 19 `composable()` route definitions from `MainActivity.kt` to `AppNavGraph.kt`.
- Update `MainActivity.kt` to call the `AppNavGraph` composable instead of defining the `NavHost` inline.
- Maintain existing callback signatures (lambdas) for screen navigation.

### Out of Scope
- Restructuring of existing routes.
- Migration of `NavArgument` handling.
- Refactoring of deep-link logic.
- Any changes to the navigation architecture or the `Routes` sealed class.

## Capabilities

### New Capabilities
None

### Modified Capabilities
None

## Approach

1. **Extraction**: Copy the `NavHost` block and associated route logic from `MainActivity` into a new file `AppNavGraph.kt` within the `com.bocatta.pos.navigation` package.
2. **Parameterization**: Define `AppNavGraph` to accept the same navigation callbacks (lambdas) currently passed to the screens in `MainActivity`.
3. **Integration**: Replace the inline `NavHost` in `MainActivity` with a call to `AppNavGraph()`, passing the required controllers and callbacks.
4. **Verification**: Perform a build and run the app to ensure all 19 routes are still accessible and the navigation flow remains identical.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `MainActivity.kt` | Modified | Remove `NavHost` implementation; call `AppNavGraph` instead. |
| `AppNavGraph.kt` | New | Dedicated home for the app's navigation graph and route definitions. |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| High file size in `AppNavGraph.kt` | High | While the file will be large (~250 lines), it is focused on a single responsibility (navigation mapping), which is preferable to a bloated `MainActivity`. |
| Regression in navigation flow | Low | Pure extraction with no logic changes. Verified by full smoke test of all 19 routes. |

## Rollback Plan

Revert the changes to `MainActivity.kt` and delete `AppNavGraph.kt` using git.

## Dependencies

- None

## Success Criteria

- [ ] `MainActivity.kt` line count is reduced by approximately 200-250 lines.
- [ ] All 19 navigation routes are functional and behave identically to the previous version.
- [ ] Project builds without errors.

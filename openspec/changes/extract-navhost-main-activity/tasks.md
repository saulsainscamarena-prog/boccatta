# Tasks: Extract NavHost from MainActivity

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~450-500 (additions + deletions) |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | single-pr |
| Chain strategy | pending |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: pending
400-line budget risk: Low

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Extract NavHost + simplify MainActivity | PR 1 | Single PR — pure extraction, no new logic |

## Phase 1: RED — Write failing tests

- [x] 1.1 Create `AppNavGraphTest` at `com.bocatta.pos.navigation` — verifies Routes sealed class contract (19 members); passes after AppNavGraph.kt exists

## Phase 2: GREEN — Create AppNavGraph + simplify MainActivity

- [x] 2.1 Create `AppNavGraph.kt` at `com.bocatta.pos.navigation` — `AppNavGraph` composable accepting `navController`, `authVm`, `sessionVm`, `cajaVm`, `inventoryVm`, `salesVmV2`, `heldOrderVm`, `mesaVm`; copy all 19 `composable()` route definitions verbatim
- [x] 2.2 Simplify `MainActivity.kt` — replace inline NavHost (lines 102–331) with `AppNavGraph(navController, authVm, sessionVm, cajaVm, inventoryVm, salesVmV2, heldOrderVm, mesaVm)`; remove 15 unused imports
- [x] 2.3 `./gradlew :app:compileDebugKotlin` — BUILD SUCCESSFUL (1m 15s)

## Phase 3: VERIFY — Test that everything still works

- [x] 3.1 `./gradlew testDebugUnitTest` — 318 tests, 316 passed, 2 pre-existing failures (OfflineManagerTest mockkStatic)
- [x] 3.2 Update verification.md

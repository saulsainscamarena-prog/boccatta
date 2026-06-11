# Tasks: Split AdminScreen — Navigation Dispatcher & Dashboard Extraction

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~1450 (mostly moved code) |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1: Navigation + extractions → PR 2: Simplify AdminScreen |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

```
Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: High
```

| Unit | Goal | Likely PR | Base |
|------|------|-----------|------|
| 1 | AdminNavigation + all tab extractions + tests | PR 1 | main |
| 2 | Strip AdminScreen to Scaffold + AdminContent | PR 2 | PR 1 branch |

## Phase 1: RED — Write Failing Tests

- [ ] 1.1 `AdminNavigationTest` — AdminSection maps string to sealed variant; AdminContent dispatches to correct composable
- [ ] 1.2 `AdminDashboardTest` — DashboardCardPremium renders title/subtitle/icon; TabDashboard shows metric cards with VM state
- [ ] 1.3 `TabBodegaGeneralTest`, `TabAuditoriaTest`, `TabRecetasTest`, `TabCostosInsumosTest`, `TabProduccionTest` — extracted composables render expected content per section

## Phase 2: GREEN — Create Extracted Files

- [ ] 2.1 `AdminNavigation.kt` — sealed class AdminSection (Dashboard, Menu, Recetas, Inventario, Empleados, Reportes, Config) + AdminContent() dispatcher with when()
- [ ] 2.2 `AdminDashboard.kt` — extract TabDashboard + DashboardCardPremium + maintenance/PIN dialogs as internal composables
- [ ] 2.3 `TabMenu.kt`, `TabBodegaGeneral.kt`, `TabAuditoria.kt`, `TabRecetas.kt`, `TabCostosInsumos.kt`, `TabProduccion.kt` — extract remaining private composables + DialogReceta into separate files under `admin/`
- [ ] 2.4 `AdminDashboardTest`, `TabBodegaGeneralTest`, `TabAuditoriaTest`, `TabRecetasTest`, `TabCostosInsumosTest`, `TabProduccionTest` — all Phase 1 tests pass with Robolectric + Compose test rule

## Phase 3: INTEGRATE — Simplify AdminScreen

- [ ] 3.1 Strip `AdminScreen.kt` — delete all private composable definitions; keep Scaffold + seccionActiva/subTabSeleccionado state + AdminContent call + imports
- [ ] 3.2 Remove unused imports from AdminScreen.kt after extraction
- [ ] 3.3 `./gradlew testDebugUnitTest` — all tests GREEN; verify AdminScreen still navigates all sections correctly

## Phase 4: REFACTOR — Cleanup

- [ ] 4.1 Remove `limpiarEtiquetaAdmin()` from AdminScreen.kt (dead code)
- [ ] 4.2 Detekt passes; no unused imports in admin/ package

All files under: `app/src/main/java/com/bocatta/pos/presentation/ui/screens/admin/`
Test files under: `app/src/test/java/com/bocatta/pos/presentation/ui/screens/admin/`

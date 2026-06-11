# Tasks: Fix Critical VM Init Blocks + Edge-to-Edge Support

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 120–160 |
| 400-line budget risk | Low |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | single-pr |
| Chain strategy | size-exception |

Decision needed before apply: No
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Low

## Phase 1: Foundation — BaseViewModel Helper

- [ ] 1.1 Modify `BaseViewModel.kt` — add `launchIO(block: suspend CoroutineScope.() -> Unit): Job` helper that launches on `viewModelScope + Dispatchers.IO + safeHandler`. (~3 lines)

## Phase 2: Core — Fix 7 ViewModel Init Blocks

- [ ] 2.1 Modify `CajaViewModel.kt` — wrap init{} listener registrations (`escucharTurnoActivo`, `escucharTotalesDia`, `verificarPendientes`, `escucharParametrosCaja`, `iniciarObservacionFallidas`) inside `launchIO { ... }` (lines 143-149). (~5 lines)
- [ ] 2.2 Modify `AdminViewModel.kt` — wrap init{} block (lines 170-196, 12 listener registrations + `cargarHistorial`) in `launchIO { ... }`. (~5 lines)
- [ ] 2.3 Modify `InventoryAdjustmentViewModelV2.kt` — wrap init{} (lines 23-26, `escucharInsumos`, `escucharMermas`) in `launchIO { ... }`. (~3 lines)
- [ ] 2.4 Modify `GestionSucursalesViewModel.kt` — wrap init{} (lines 50-52, `escucharSucursales`) in `launchIO { ... }`. (~3 lines)
- [ ] 2.5 Modify `DevolucionViewModel.kt` — wrap init{} (lines 28-30, `escucharSolicitudes`) in `launchIO { ... }`. (~3 lines)
- [ ] 2.6 Modify `MesaViewModel.kt` — wrap init{} (lines 27-30) in `launchIO { ... }` to defer `cargarZonas()` and `cargarMesas()`. (~3 lines)
- [ ] 2.7 Modify `ConfigNegocioViewModel.kt` — wrap init{} (line 38-40, `cargarDatos()`) in `launchIO { ... }`. (~3 lines)

## Phase 3: Feature — EdgeToEdgeScaffold

- [ ] 3.1 Create `presentation/ui/components/EdgeToEdgeScaffold.kt` — reusable composable wrapping `Scaffold` with `contentWindowInsets = WindowInsets.safeDrawing` and propagating all params (topBar, bottomBar, content, etc.). (~30 lines)
- [ ] 3.2 Modify `ActividadScreen.kt` — add `contentWindowInsets = WindowInsets.safeDrawing` to `Scaffold` at line 77 (or swap to `EdgeToEdgeScaffold`). (~1 line)
- [ ] 3.3 Modify `OnboardingScreen.kt` — add `contentWindowInsets = WindowInsets.safeDrawing` to `Scaffold` at line 19 (or swap to `EdgeToEdgeScaffold`). (~1 line)

## Phase 4: Testing

- [ ] 4.1 Write unit test for `BaseViewModel.launchIO` — verify it launches on IO dispatcher, catches exceptions via `safeHandler`, and sets `cargando` true/false. (~20 lines)
- [ ] 4.2 Write unit tests for each of the 7 ViewModels — verify `init {}` does not block and data loads asynchronously (test `cargando` lifecycle, check data is populated). (~40 lines)
- [ ] 4.3 Run full test suite: `./gradlew testDebugUnitTest`. Fix any failures.

## Implementation Order

Phase 1 (BaseViewModel helper) → Phase 2 (7 ViewModel patches, order-independent) → Phase 3 (EdgeToEdgeScaffold + 2 screen patches) → Phase 4 (tests). Phases 2 and 3 are independent and could be parallelized. Phase 4 blocks on all previous phases.

# Tasks: Caja — Retiros Parciales y Arqueo por Denominación

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~355 |
| 400-line budget risk | Medium |
| Chained PRs recommended | No |
| Suggested split | Single PR |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: No (owner approved — single PR)
Chained PRs recommended: No
Chain strategy: size-exception
400-line budget risk: Medium

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Models + constants + AccionSensible + ViewModel + UI + tests | PR 1 (single) | Entire change in one PR |

## Phase 1: Domain Models + Constants

- [x] 1.1 Add `RetiroParcialV2` data class to `domain/model/ModelsV2.kt` (id, monto, motivo, timestamp, usuarioId)
- [x] 1.2 Add `cajaId: String = "1"` and `denominacionesContadas: Map<String, Int> = emptyMap()` to `TurnoCajaV2`
- [x] 1.3 Add `RETIROS = "retiros"` constant to `core/constants/FirestoreCollections.kt`
- [x] 1.4 Add `GASTAR_CAJA` entry to `AccionSensible` enum in `domain/usecase/AuthorizationManager.kt`

## Phase 2: ViewModel — CajaViewModel.kt

- [x] 2.1 Add `denominacionesInput: SnapshotStateMap<String, String>` state for denomination grid inputs
- [x] 2.2 Add `retiroList: List<RetiroParcialV2>` state and subscribe to RETIROS sub-collection listener
- [x] 2.3 Add `registrarRetiroParcial(pin, monto, motivo, usuarioNombre, onResult)` — validates PIN via `authManager.validarConPinYAuditar(AccionSensible.GASTAR_CAJA)`, runs Firestore transaction: create retiro doc in sub-collection + increment `totalGastosTurno`
- [x] 2.4 Add `actualizarDenominaciones(map: Map<String, Int>)` — updates `denominacionesInput` and computes `efectivoContado` from denomination multiplication
- [x] 2.5 Refactor `cerrarTurno()` — replace inline `diferenciaCaja`/`cadraCaja` with `CuadreCajaManager.calcularCuadre()`, write `denominacionesContadas` + `cajaId` to Firestore on close

## Phase 3: UI — CierreCajaScreen.kt

- [x] 3.1 Add denomination grid below `efectivoContado` OutlinedTextField: rows of denomination chips (1000, 500, 200, 100, 50, 20, 10, 5, 2, 1, 0.50, 0.20, 0.10) with editable quantity `OutlinedTextField` each; computed total displayed below grid
- [x] 3.2 Add "Registrar Retiro" button in the screen, opens PIN dialog → on success opens retiro AlertDialog with monto + motivo fields → calls `registrarRetiroParcial`
- [x] 3.3 Wire denomination-based `efectivoContado` computed total into the existing cerrarTurno flow (as `efectivoContado` input value) — done via `actualizarDenominaciones()` updating the shared `efectivoContado` state

## Phase 4: Tests

- [x] 4.1 Add parameterised tests to `CuadreCajaManagerTest` covering denomination scenario totals (e.g. 5×100 + 3×50 = 650, with tolerance checks)
- [x] 4.2 Add unit tests to `CajaViewModelLogicTest` for denomination sum calculation (map of denom→qty → expected total)
- [x] 4.3 Add unit test for `RetiroParcialV2` default values and construction

## Phase 5: Compile + Verify

- [x] 5.1 Run `compileDebugKotlin` — SUCCESS (after fixes)
- [x] 5.2 Run `testDebugUnitTest` — all tests pass

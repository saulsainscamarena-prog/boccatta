# Tasks: Clientes/CRM — Lealtad y Ciclo de Visitas

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~300 |
| 400-line budget risk | Medium |
| Chained PRs recommended | No |
| Delivery strategy | ask-on-risk |

Decision needed before apply: No
Chained PRs recommended: No

## Phase 1: MembresiaDiscountCalculator + Tests

- [x] 1.1 Create `MembresiaDiscountCalculator` object in `domain/` with `calcularPorcentajeDescuento(promedio)` and `calcularPromedio(compras)`
- [x] 1.2 Create `MembresiaDiscountCalculatorTest` with parameterised tests for each bracket (<100, 100-299, 300-499, 500+) and edge cases (empty list, zero, exact boundaries)
- [x] 1.3 Verify MembresiaManagerTest still passes (pre-existing 4 tests) ✓

## Phase 2: MembresiaRepository

- [x] 2.1 Create `MembresiaRepository` in `data/repository/` with `obtenerCliente(id)`, `incrementarVisita(id, monto, premioAplicado)`, `resetearCiclo(id)`
- [x] 2.2 Add Firestore transaction helper for atomic visit increment + comprasCicloActual update + optional reset

## Phase 3: Checkout Integration

- [x] 3.1 Add loyalty check to SalesViewModelV2: after selecting cliente, call MembresiaManager + MembresiaDiscountCalculator
- [x] 3.2 Add `descuentoLealtad: Double` state to SalesViewModelV2, compute from discount %
- [x] 3.3 Wire `descuentoLealtad` into CheckoutUseCase/total calculation
- [x] 3.4 After sale completion, call MembresiaRepository.incrementarVisita + resetearCiclo if premio applied

## Phase 4: UI

- [x] 4.1 Add level badge (PLATA/ORO/PLATINO) to ClienteIndustrialCard in ClientesScreen
- [x] 4.2 Add visit progress indicator "Visita X de 5" in client detail
- [x] 4.3 Add loyalty notification banner in SalesScreen when cliente eligible
- [x] 4.4 Show descuentoLealtad line in cart summary

## Phase 5: Compile + Verify

- [x] 5.1 Run `compileDebugKotlin` — PASS
- [x] 5.2 Run `testDebugUnitTest` — 317 tests, 2 pre-existing OfflineManagerTest failures only

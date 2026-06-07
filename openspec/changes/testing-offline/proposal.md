# Proposal: Offline Tests for SalesViewModelV2 and InventoryDeductions

Spec ID: `testing-offline`
Capability: `testing`
Strict TDD: `no`

## Problema

The offline fallback for cart management, inventory deductions, and UI state transitions inside `SalesViewModelV2` has zero unit and instrumented testing coverage. Additionally, `InventoryDeductions` lacks unit tests covering its newer dynamic `configSchema` deduction resolution logic. This creates a risk of silent failures when making changes to POS offline behavior.

## Alcance

- Add unit tests for `InventoryDeductions` verifying `resolverDescuentoOpcion` mapping dynamically via `configSchema`.
- Create unit tests for `SalesViewModelV2` using mocks for dependencies like `CheckoutUseCase` and `CartManager`, verifying its behavior when operating without a network.
- Create an instrumented test (e.g. `OfflineCheckoutInstrumentedTest.kt`) to test the real integration of `SalesViewModelV2` with `Room` (offline database) and verify deducciones are correctly calculated and enqueued when offline.

## Fuera de Alcance

- Modifying the underlying offline sync logic or `SyncWorker`.
- Modifying UI Composables or creating Compose UI tests.

## Archivos Afectados Esperados

- `app/src/test/java/com/bocatta/pos/data/repository/InventoryDeductionsTest.kt` (Update)
- `app/src/test/java/com/bocatta/pos/presentation/viewmodel/SalesViewModelV2Test.kt` (New)
- `app/src/androidTest/java/com/bocatta/pos/OfflineCheckoutInstrumentedTest.kt` (New)

## Riesgos

- Ventas: None (only adding tests).
- Inventario: None.
- Offline/sync: None.
- Caja/pagos: None.
- UI: None.
- Seguridad: None.

## Research Externo

- Web requerida: no
- Pregunta investigada: N/A
- Fuentes primarias: N/A
- Decision tomada: Base on standard Android testing patterns (JUnit, MockK, AndroidX Test).
- Suposicion sensible a version: N/A

## Estrategia de Rollback

- Delete the newly added test files or revert the test class changes if they cause CI/CD or local build issues.

## Criterios de Exito

- [ ] `InventoryDeductionsTest.kt` covers `configSchema`.
- [ ] `SalesViewModelV2Test.kt` covers offline state preservation and cart updates.
- [ ] `OfflineCheckoutInstrumentedTest.kt` covers the end-to-end integration of ViewModel state saving to Room.
- [ ] All tests pass without flakiness.

## Checkpoint Humano

No pasar a `spec.md` y `design.md` hasta que esta propuesta este aceptada o ajustada.

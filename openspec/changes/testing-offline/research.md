# Research: Testing Gap for SalesViewModelV2 and InventoryDeductions in Offline Scenarios

Spec ID: `testing-offline`
Capability: `testing`
Fecha: `2026-06-06`

## Brief Humano

The Bocatta POS system needs to operate seamlessly both online and offline. The offline mode uses an `OfflineDatabase` and a `SyncWorker` to preserve sales and inventory deductions locally until the connection is restored. This research focuses on identifying the testing gaps in `SalesViewModelV2` and `InventoryDeductions` concerning the offline flow.

## Web Research Gate

- Research question: N/A
- Source category from `openspec/source-catalog.yaml`: N/A
- Why local context is insufficient: Local context is sufficient.
- Expected decision: N/A
- Stop condition: N/A
- Budget: none

## Contexto Local Leido

- Constitution: `AGENTS.md` rules on Sales, Inventory, and Offline.
- Archivos inspeccionados:
  - `SalesViewModelV2.kt`
  - `InventoryDeductions.kt`
  - `InventoryDeductionsTest.kt`

## Alternativas

### Opcion A

- Descripcion: 
1. **`InventoryDeductions`**: 
   - Has substantial unit testing coverage in `InventoryDeductionsTest.kt` (425 lines of tests).
   - **Gap**: There are no unit tests verifying the dynamic `configSchema` mappings logic inside `resolverDescuentoOpcion`.
2. **`SalesViewModelV2`**:
   - Currently, it has virtually **no unit tests** and **no instrumented tests**.
   - **Gap**: The entire state flow, offline mode adaptation within the UI state (`isOnline` handling), and cart state preservation during outages are untested. 

- Ventajas: Ensures coverage for the exact gaps identified.
- Costos: Normal test implementation time.
- Riesgos: None.

## Decision Recomendada

Follow Opcion A. Implement unit tests for `SalesViewModelV2` and `InventoryDeductions` (specifically `configSchema` logic), and create instrumented tests to verify `SalesViewModelV2` behaviors when operating offline.

## Suposiciones Externas

- Dependencies like MockK and testing dispatchers are already configured.

## Tradeoffs

- Time spent on test infrastructure versus feature development, but highly necessary for stability.

## Preguntas Que Deben Aclararse Antes de Proponer

- None.

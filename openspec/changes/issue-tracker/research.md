# Research - Issue Tracker Audit

## Context
An audit of open bugs, issues, and technical debt markers was conducted across the `bocatta` Android project. The audit included reviewing existing documentation (`TECH_DEBT.md`, `MASTER_CONTEXT_FIX_PLAN.md`, `plan de modificaciones aprobadas.txt`, `pendientes_gemini.txt`) and scanning the codebase for `TODO`, `FIXME`, and `HACK` markers.

## Findings

### 1. Codebase Markers (TODOs)
A `grep` search for `TODO` across `app/src` returned over **261 results**. Notable patterns include:
- **Hardcoded Default Values:** e.g., `metodoPago: String = MetodoPago.EFECTIVO.valor`.
- **Type Conversions:** Frequent `.toDouble()` conversions in calculations across `PricingEngine`, `InventoryDeductions`, and `FirebaseSalesRepositoryV2`.
- **Seeding and Initialization:** Notes in `DataSeederV2` regarding the creation of catalogs and stock allocation.
- **Repository Abstractions:** `ProductoRepository` mentions that "the catalog is global for now, but this method prepares the architecture."

### 2. Technical Debt (`TECH_DEBT.md`)
Most major technical debt items have been resolved (Room via KSP2, Legacy model consolidation, SharedPreferences replaced with SQLite). However, there are pending items for version 2.2:
- **Firestore Listeners:** Migrate ViewModel Firestore listeners to `IProductRepository.getAllProducts(): Flow<InventoryProductV2>`.
- **Dynamic Multi-giro Form Engine:** Load real `product_definitions` from Firestore to validate non-food giro rendering on physical devices.

### 3. Master Context Fix Plan (`MASTER_CONTEXT_FIX_PLAN.md`)
Six critical presentation/UI issues were reported that need fixing without touching `domain/` or `data/` layers:
1. `SalesViewModelV2.kt`: `onCobrar()` fails silently. Needs state updates (`isLoading`, `error`, `showSuccess`).
2. `SalesScreen.kt`: Missing Snackbars/dialogues. UI doesn't react to ViewModel states.
3. `ProductionRegistrationDialog.kt`: Confusing input ("Materia prima usada"). Should only ask for "Yield/Porciones".
4. `DynamicFormEngine.kt`: Shows JSON keys or broken UTF-8 chars in labels.
5. `CrepeBuilderDialog.kt`: Single-choice only. Needs `FlowRow` with `FilterChip` for multiselection.
6. `root/`: Missing `git init` and base snapshot commit.

### 4. Approved Modifications Plan (`plan de modificaciones aprobadas.txt`)
Four functional blockages affecting operations:
1. **SalesScreen Empty Catalog:** `SalesViewModelV2.escucharMenu()` doesn't assign `catalogoProcesado`, causing empty screens.
2. **Held Orders / Tables Stuck:** Post-sale cleanup clears `activeHeldOrderId` too early, leaving tables occupied or orders un-deleted.
3. **Data Loss on Held Orders:** The old held orders dialog deletes the order immediately upon loading, meaning it's lost if the charge fails or is cancelled.
4. **Table Re-reservation Race Condition:** Re-saving a table order deletes the old one, potentially freeing the table incorrectly due to an asynchronous race.

## Constraints
- **Do not modify** `domain/` or `data/` layers. Fixes must be applied surgically in `presentation/`.
- Ensure tests still pass (`./gradlew testDebugUnitTest`).
- Preserve responsive layout (720dp breakpoint) and use `MaterialTheme` colors.
- Maintain atomic offline syncing rules.

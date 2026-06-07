# Proposal - Issue Tracker and Tech Debt Resolution

## Goal
Address the accumulated technical debt, functional blockages, and UI feedback outlined in the research phase systematically, ensuring stability and a smooth UX for the Bocatta POS v2 application. 

## Strategy
We propose addressing the issues in three prioritized phases to minimize risk and ensure functional correctness for the point-of-sale operations. 

### Phase 1: Critical Functional & Operational Blockages
These issues actively prevent the POS from functioning correctly during sales or table management (from `plan de modificaciones aprobadas.txt`).
1. **Fix SalesScreen Catalog Load:** Assign `catalogoProcesado = catalogoUseCase.clasificarYOrdenar(...)` in `SalesViewModelV2.escucharMenu()`.
2. **Fix Held Order Deletion:** Retain `heldOrderId` and `mesaId` during checkout; only delete the order and free the table *after* a successful transaction.
3. **Safe Held Order Retrieval:** Modify the held orders dialog to load orders using `salesVm.cargarOrdenEnCarrito(...)` without deleting them from SQLite until charged.
4. **Fix Table Re-reservation Race:** Synchronize the deletion of old orders and creation of new ones to prevent tables from being erroneously marked as `LIBRE`.

### Phase 2: UI Feedback and Presentation Fixes
These issues affect operator experience and feedback (from `MASTER_CONTEXT_FIX_PLAN.md`).
1. **Sales UI States:** Add `try/catch` in `SalesViewModelV2.onCobrar()`, and expose `isLoading`, `error`, and `showSuccess` states. Update `SalesScreen.kt` to consume these states and show Snackbars.
2. **Simplify Production Registration:** Refactor `ProductionRegistrationDialog.kt` to ask solely for the numeric yield.
3. **Fix UTF-8 in Forms:** Apply regex filtering `Regex("[^\\x00-\\x7F]")` to labels in `DynamicFormEngine.kt` to prevent mojibake.
4. **Enable Crepe Multiselection:** Refactor `CrepeBuilderDialog.kt` to use a `FlowRow` of `FilterChip` components instead of RadioButtons.
5. **Git Initialization:** Run `git init` and create a base snapshot if not already present.

### Phase 3: Technical Debt and Codebase Cleanup
These issues are maintenance tasks (from `TECH_DEBT.md` and `TODO` scans).
1. **Migrate Firestore Listeners:** Refactor ViewModels to consume `IProductRepository.getAllProducts(): Flow<InventoryProductV2>` instead of direct Firestore listeners.
2. **Dynamic Form Definitions:** Connect `DynamicFormEngine` to load real `product_definitions` from Firestore.
3. **TODO Cleanup:** Iteratively review and resolve the 261+ `TODO` comments in the `app/src` directory, starting with hardcoded `metodoPago` references and `.toDouble()` calculation cleanup.

## Impact
- **No data layer changes:** The robust `data` and `domain` logic remains untouched.
- **Improved UX:** Operators will have clear feedback (Snackbars) and simpler flows (Production, Crepes).
- **Stability:** Tables and held orders will no longer glitch during payment or re-reservation.

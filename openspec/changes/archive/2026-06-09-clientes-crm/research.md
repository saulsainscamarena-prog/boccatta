## Exploration: Clientes/CRM Module

### Current State
The project already includes a basic client management feature used primarily in the sales flow. A `ClienteV2` data class models a client with fields like `idDocumento`, `nombre`, and `telefono`. Clients are stored in a Firestore collection accessed via `CustomerRepository`. The UI provides a client selection dialog, a screen for managing clients (`ClientesScreen`), and integration points in the sales checkout (`SalesViewModelV2`, `CheckoutUseCase`).

### Data Models
- **`domain/model/ModelsV2.kt`** – `data class ClienteV2(idDocumento: String = "", nombre: String = "", telefono: String = "")`
  - Fields: `idDocumento`, `nombre`, `telefono`.
  - No loyalty, points, visit count, or tier information.

### Firestore Collections
- **`data/FirestoreCollections.kt`** (not fully listed, but referenced constants) includes a collection for clients (`CLIENTES` / `CUSTOMERS`). No explicit collections for loyalty, visit cycles, or membership tiers.

### Repositories & Use Cases
- **`data/repository/CustomerRepository.kt`**
  - `buscarCliente(query: String): List<ClienteV2>` – simple text search.
  - `registrarClienteNuevo(nombre, telefono)` – creates a new `ClienteV2` document.
  - No repository for loyalty or membership.
- **`domain/usecase/CheckoutUseCase.kt`** – receives an optional `clienteSeleccionado: ClienteV2?`.
- **`presentation/viewmodel/SalesViewModelV2.kt`** – holds mutable state `_clienteSeleccionado`, provides functions `buscarCliente`, `seleccionarCliente`, `eliminarCliente`, `registrarClienteNuevo`.
- **`presentation/viewmodel/ClienteViewModel.kt`** – manages client list, CRUD operations, and UI messages.

### UI / Screens
- **`presentation/ui/screens/clientes/ClientesScreen.kt`** – full screen to list, edit, delete, and add clients.
- **`presentation/ui/components/CarritoPanelV2.kt`**, **`DialogosVentas.kt`**, **`SalesScreen.kt`** – dialogs for searching and registering clients during a sale.
- Navigation entry added in **`MainActivity.kt`** under `Routes.Clientes`.

### Integration in Sales / Checkout
- Sales flow (`SalesScreen`, `SalesViewModelV2`, `CheckoutUseCase`) passes the selected `ClienteV2` to the backend (`FirebaseSalesRepositoryV2`).
- No discount logic tied to client loyalty; the client object is only persisted for audit purposes.

### Loyalty Features (Existing)
- The codebase contains a test `MembresiaManagerTest.kt` and a constant `MEMBRESIA_CICLO_VISITAS = 5` in Firestore constants, suggesting an intended visit‑cycle loyalty system, but no concrete implementation:
  - No model fields for visits, points, or tier.
  - No repository or use‑case that updates or reads a loyalty counter.
  - No UI components displaying loyalty status or applying discounts.

### String Resources
- Search in `res/values/strings.xml` reveals keys like `cliente`, `registrar_cliente`, but no loyalty‑specific strings (e.g., "puntos", "beneficios", "nivel").

### Gaps & Opportunities
| Area | Gap | Opportunity |
|------|-----|-------------|
| **Data Model** | No loyalty/points/tier fields. | Extend `ClienteV2` (or create a separate `MembresiaV2`) with `visitas`, `puntos`, `nivel`, `fechaUltimaVisita`.
| **Firestore** | No dedicated collections for loyalty history or membership tiers. | Add `MEMBRESIAS` collection; store a document per client with loyalty counters.
| **Repository / Use‑case** | No logic to increment visits, calculate points, or apply discounts. | Implement `MembresiaRepository` and `MembresiaUseCase` to manage visit cycles and reward calculations.
| **UI** | Only client CRUD; no loyalty display or rewards UI. | Add loyalty badge on `ClienteIndustrialCard`; show points on checkout; provide "Canje" dialog.
| **Sales Flow** | Loyalty not considered when creating a ticket. | Modify `CheckoutUseCase` to query loyalty status, apply percentage discount automatically, and record the visit.
| **String Resources** | Missing localization for loyalty terms. | Add strings for "Puntos", "Nivel", "Beneficio", etc.
| **Testing** | No unit/instrumented tests for loyalty logic. | Add tests around `MembresiaManager` (currently a test placeholder) to verify visit increment and reward thresholds.

### Recommended MVP Scope
1. **Model Extension** – Add `visitas:Int = 0`, `puntos:Int = 0`, `nivel:String = ""` to `ClienteV2` (or create `MembresiaV2`).
2. **Firestore** – Create `MEMBRESIAS` collection; migrate existing clients with default values.
3. **Repository** – `MembresiaRepository` with methods `incrementVisita(clienteId)`, `addPuntos(clienteId, amount)`, `getMembresia(clienteId)`.
4. **Use‑case** – `MembresiaManager` that enforces `MEMBRESIA_CICLO_VISITAS` (e.g., every 5 visits grant a discount).
5. **UI** – Show loyalty badge on client list; in `SalesScreen` display points and auto‑apply discount when eligible.
6. **Checkout Integration** – Before finalising a sale, fetch loyalty, apply discount if threshold reached, and record the visit.
7. **Tests** – Unit tests for `MembresiaManager`; instrumented UI tests for loyalty display.

### Integration Points
- **ClienteViewModel** – load and expose loyalty data alongside client list.
- **SalesViewModelV2** – after selecting a client, request loyalty status and compute potential discount.
- **CheckoutUseCase** – receives `ClienteV2?`; now also receives `MembresiaV2?` to apply discounts.
- **FirebaseSalesRepositoryV2** – persist loyalty updates together with the sale document.

### Next Steps
- Review `FirestoreCollections.kt` to confirm constant names and create missing collection constants.
- Draft a migration script to add loyalty fields to existing client documents.
- Prototype `MembresiaManager` and integrate with the sales flow.
- Align UI with Material 3 design using the `styles` skill for loyalty badges.

**Ready for Proposal**: Yes – the above research provides a concrete foundation for a CRM/loyalty MVP. The orchestrator can now move to the proposal phase.

# Design: Auditoría Integral y Saneamiento

## Technical Approach

Implement comprehensive fixes for critical issues across the codebase while preserving the existing MVVM + Clean Architecture, Koin DI, and Firebase/Room integrations. Each fix introduces proper coroutine handling, error propagation, atomic transactions, and debouncing where needed. New helper classes and use‑cases are added to encapsulate reusable logic, and a process‑gate framework is introduced to orchestrate SDD change state.

## Architecture Decisions

### Decision: Coroutine Handling
**Choice**: Replace all `runBlocking` on UI or background threads with structured concurrency using `viewModelScope` or `CoroutineScope(Dispatchers.IO)` and `suspend` functions.
**Alternatives considered**: Retaining `runBlocking` with `Dispatchers.Default` – rejected due to UI thread blocking risk.
**Rationale**: Ensures UI responsiveness and aligns with Clean Architecture's use‑case driven coroutines.

### Decision: Error Propagation
**Choice**: Remove empty `catch {}` blocks; propagate exceptions to a centralized error handler via `Result` wrappers or sealed classes.
**Alternatives considered**: Logging only – insufficient for upstream recovery.
**Rationale**: Guarantees visibility of failures and enables retry or UI feedback.

### Decision: Atomic Transactions
**Choice**: Use Room `@Transaction` and Firestore batch writes for multi‑step operations.
**Alternatives considered**: Manual roll‑backs – error‑prone.
**Rationale**: Guarantees all‑or‑nothing semantics.

### Decision: Debounce / Gate Locks
**Choice**: Introduce `DebounceHelper` and `ProcessGate` to serialize critical actions like checkout and sync.
**Alternatives considered**: Simple boolean flags – not reusable across components.
**Rationale**: Centralised, testable, and reusable across ViewModels and Workers.

## Data Flow

```
UI (Fragment/Activity) → ViewModel → UseCase → Repository → DataSource (Room / Firestore)
```

Critical fixes add a `Result`/`Either` wrapper flowing back to UI for error handling and introduce `ProcessGate` checks before invoking use‑cases.

## File Changes

| File | Action | Description |
|------|--------|-------------|
| `app/src/main/java/com/bocatta/viewmodel/SalesViewModelV2.kt` | Modify | Replace `runBlocking` with `viewModelScope.launch`; add debounce guard; cancel jobs in `onCleared`; propagate errors via `Result`.
| `app/src/main/java/com/bocatta/usecase/CheckoutUseCase.kt` | Modify | Add debounce via `DebounceHelper`; wrap checkout call in a transaction; return `Result`.
| `app/src/main/java/com/bocatta/repository/InventoryRepository.kt` | Modify | Implement `@Transaction` for stock deduction; add connectivity check via `NetworkHelper`.
| `app/src/main/java/com/bocatta/usecase/InventoryDeductionsUseCase.kt` | Modify | Wrap logic in `suspend` with proper try/catch; return `Result`.
| `app/src/main/java/com/bocatta/manager/OfflineManager.kt` | Modify | Replace `runBlocking` with `CoroutineScope(Dispatchers.IO)`; use `@Transaction` for batch inserts; remove `!!` assertions.
| `app/src/main/java/com/bocatta/worker/SyncWorker.kt` | Modify | Extend `CoroutineWorker`; remove `runBlocking`; add inventory sync step; use `ProcessGate`.
| `app/src/main/java/com/bocatta/util/DebounceHelper.kt` | New | Helper to debounce rapid calls (e.g., checkout, sync).
| `app/src/main/java/com/bocatta/util/ProcessGate.kt` | New | Generic gate to mark SDD process steps as active/inactive; supports auto‑archive of inactive changes.
| `app/src/main/java/com/bocatta/util/NetworkHelper.kt` | New | Checks connectivity before remote operations.
| `app/src/main/java/com/bocatta/usecase/ProcessGateUseCase.kt` | New | Use‑case exposing gate API to ViewModels/Workers.
| `app/src/main/java/com/bocatta/di/AppModule.kt` | Modify | Provide new helpers in Koin module.
| `app/src/main/java/com/bocatta/worker/SyncWorker.kt` | Modify | Add inventory sync after sales sync.
| `app/src/main/java/com/bocatta/worker/SyncWorker.kt` | Modify | Cancel/skip when `ProcessGate` indicates no pending changes.
| `app/src/main/java/com/bocatta/worker/SyncWorker.kt` | Modify | Return proper `Result.success()` or `Result.retry()`.
| `app/src/main/java/com/bocatta/worker/SyncWorker.kt` | Modify | Use `CoroutineWorker` instead of `Worker`.
| `app/src/main/java/com/bocatta/model/ResultWrapper.kt` | New | Sealed class for success/failure states.
| `app/src/main/java/com/bocatta/usecase/CheckoutUseCase.kt` | Modify | Return `ResultWrapper`.
| `app/src/main/java/com/bocatta/viewmodel/SalesViewModelV2.kt` | Modify | Observe `ResultWrapper` and display UI errors.

## Interfaces / Contracts

```kotlin
// Result wrapper used across use‑cases
sealed class ResultWrapper<out T> {
    data class Success<out T>(val data: T) : ResultWrapper<T>()
    data class Failure(val throwable: Throwable) : ResultWrapper<Nothing>()
}

interface DebounceHelper {
    fun <T> debounce(key: String, intervalMs: Long = 500, block: suspend () -> T): Deferred<T>
}

interface ProcessGate {
    fun isOpen(step: String): Boolean
    fun close(step: String)
    fun open(step: String)
    fun autoArchiveInactive(timeoutMs: Long = 24 * 60 * 60 * 1000)
}
```

## Testing Strategy

| Layer | What to Test | Approach |
|-------|---------------|----------|
| Unit | `DebounceHelper`, `ProcessGate`, new repository methods | JUnit + MockK, coroutine test dispatcher |
| Unit | Use‑cases (`CheckoutUseCase`, `InventoryDeductionsUseCase`) | Verify atomic transaction behavior, proper `ResultWrapper` propagation |
| Integration | ViewModel – UI interaction, error handling | Robolectric/Compose UI tests, mock repositories |
| Integration | `SyncWorker` execution flow | WorkManager test library with `TestCoroutineDispatcher` |
| E2E | Full checkout flow including debounce and cancelation | Espresso UI test with mocked network connectivity |

## Migration / Rollout

- Deploy a feature flag (`sddAuditFixesEnabled`) via Remote Config to toggle new logic.
- Initial release enables fixes for `SalesViewModelV2` and `SyncWorker`; subsequent releases roll out `InventoryDeductions` and `OfflineManager` fixes.
- No data migration required; atomic transactions prevent inconsistencies.

## Open Questions
- [ ] Should we centralise all `ResultWrapper` handling in a base `BaseViewModel`?
- [ ] Do we need a retry policy for Firestore batch writes beyond WorkManager's retry?
- [ ] Confirm the exact debounce interval for checkout based on UX research.

---

**Implementation Order**
1. Add `DebounceHelper`, `ProcessGate`, `ResultWrapper`, and Koin bindings.
2. Fix `SalesViewModelV2` (runBlocking, job cancel, debounce, error propagation).
3. Update `CheckoutUseCase` to use debounce and transactional checkout.
4. Refactor `SyncWorker` to `CoroutineWorker` and add inventory sync.
5. Implement `NetworkHelper` and enhance `InventoryRepository` with `@Transaction`.
6. Fix `InventoryDeductionsUseCase` and related repository methods.
7. Refactor `OfflineManager` (runBlocking removal, transaction, null‑assertion safety).
8. Add auto‑archive logic to `ProcessGate` and close remaining truncated changes.
9. Wrap up with feature‑flag rollout and comprehensive testing.

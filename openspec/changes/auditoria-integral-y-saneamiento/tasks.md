# Tasks: Auditoría Integral y Saneamiento

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | 1,500–2,500 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 → PR 2 → PR 3 → PR 4 |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Infrastructure helpers + SalesViewModelV2 fixes | PR 1 | Base: main. DebounceHelper, ProcessGate, ResultWrapper, NetworkHelper, Koin DI, más SalesViewModelV2 + CheckoutUseCase |
| 2 | SyncWorker + OfflineManager fixes | PR 2 | Base: main. CoroutineWorker, transacciones Room, null-safety, inventario pendiente |
| 3 | InventoryDeductions + firestore atomicas | PR 3 | Base: main. Connectivity check, runTransaction, rollback, Timber.e |
| 4 | Gates de proceso + Saneamiento + Tests | PR 4 | Base: main. Soft gate, auto-archive, offline gate, 18 cambios truncados, tests unitarios |

## Phase 1: Infrastructure / Helpers

- [ ] 1.1 Crear `util/DebounceHelper.kt` — debounce genérico con ConcurrentHashMap + interval configurable + testabilidad
- [ ] 1.2 Crear `util/ProcessGate.kt` — gate con estado activo/inactivo + autoArchiveInactive() + testabilidad
- [ ] 1.3 Crear `util/NetworkHelper.kt` — wrapper sobre ConnectivityManager con check isOnline() + testabilidad
- [ ] 1.4 Crear `model/ResultWrapper.kt` — sealed class Success/Failure + map/extensions
- [ ] 1.5 Registrar helpers en `di/AppModule.kt` como singletons Koin

## Phase 2: SalesViewModelV2 — Checkout Fixes (P0)

- [ ] 2.1 **A1**: Agregar `isProcessingCheckout: AtomicBoolean` en `SalesViewModelV2` + debounce 300ms en botón Cobrar
- [ ] 2.2 **A2**: Reemplazar `runBlocking` en UI thread por `viewModelScope.launch` + StateFlow de resultado
- [ ] 2.3 **A3**: Reemplazar `catch {}` vacío por `Timber.e` + propagación de error al UI state via `ResultWrapper`
- [ ] 2.4 **A4**: Cancelar `menuJob` + `stockJob` en `onCleared()` con chequeo `isActive`
- [ ] 2.5 **A5**: Agregar chequeo de estado loading/error/empty en `SalesUiState` + observar `ResultWrapper`

## Phase 3: SyncWorker — Fixes de Sincronización (P0)

- [ ] 3.1 **D1**: Migrar `SyncWorker` de `Worker` a `CoroutineWorker` con `doWork()` suspend
- [ ] 3.2 **D2**: Eliminar `runBlocking`, usar `coroutineScope` + suspend functions
- [ ] 3.3 **D3**: Reemplazar `catch {}` vacío por `Timber.e` + `Result.retry()` con backoff
- [ ] 3.4 **D4**: Agregar sync de inventario — llamar a `InventoryRepository.syncPendingDeductions()` al final

## Phase 4: InventoryDeductions — Fixes de Inventario (P1)

- [ ] 4.1 **B1**: Agregar verificación de conectividad via `NetworkHelper` antes de deducir en Firestore
- [ ] 4.2 **B2**: Envolver `deductStock()` en try/catch con `Timber.e` + fallback offline a Room
- [ ] 4.3 **B3**: Cambiar writes secuenciales a `Firestore.runTransaction()` para atomicidad
- [ ] 4.4 **B4**: Agregar rollback/compensación si falla deducción de lote (reversa manual)
- [ ] 4.5 **B5**: Reemplazar `catch {}` vacío en todo el use-case por `Timber.e` + Result

## Phase 5: OfflineManager — Fixes de Persistencia (P1)

- [ ] 5.1 **C1**: Reemplazar `runBlocking` en Room insert por suspend function + `Dispatchers.IO`
- [ ] 5.2 **C2**: Envolver insert + upload en transacción Room `@Transaction`
- [ ] 5.3 **C3**: Reemplazar `catch {}` vacío por `Timber.e` + mantener entrada en cola
- [ ] 5.4 **C4**: Reemplazar `!!` con `?.let` + manejo de null seguro con logging

## Phase 6: Proceso SDD — Gates + Saneamiento (P2)

- [ ] 6.1 **E1**: Definir soft gate (3+ cambios abiertos requieren justificación en proposal.md)
- [ ] 6.2 **E1**: Implementar auto-archive (14 días sin actividad → archive.md con status suspended)
- [ ] 6.3 **E1**: Agregar offline gate en verification.md para cambios que toquen sales/inventory/sync
- [ ] 6.4 **E2**: Revisar cada uno de los 18 cambios truncados en `openspec/changes/` — si irrelevante → cancel.md con justificación
- [ ] 6.5 **E2**: Para cambios truncados críticos → completar spec.md mínimo + archive.md
- [ ] 6.6 **E3**: Documentar gates en `constitution.md` o en `openspec/specs/governance-rules/spec.md` (merge)

## Phase 7: Testing (P1)

- [ ] 7.1 **F1**: Tests unitarios para protección de doble cobro — verificar AtomicBoolean + debounce con coroutine test
- [ ] 7.2 **F2**: Tests unitarios para `InventoryDeductions` con transacciones — MockK + runBlockingTest
- [ ] 7.3 **F3**: Tests unitarios para `OfflineManager` con fallos de red — verificar cola preservada
- [ ] 7.4 **F4**: Tests para `SyncWorker` migrado a CoroutineWorker — WorkManager TestDriver + estado retry

## Verification Criteria por Tarea

| ID | Verificación |
|----|-------------|
| A1 | `isProcessingCheckout.get()` es true durante cobro y false al terminar; segundo click no ejecuta cobro |
| A2 | ViewModel no usa `runBlocking`; cobro corre en `viewModelScope` |
| A3 | Error de red se muestra en UI como estado error; `Timber.e` lo loguea |
| A4 | `menuJob.cancel()` y `stockJob.cancel()` se llaman en `onCleared` |
| A5 | `SalesUiState` tiene estado loading/error/empty y UI reacciona |
| B1 | `NetworkHelper.isOnline()` se invoca antes de Firestore write |
| B2 | Fallo de red deriva a Room offline queue |
| B3 | Deducción lote usa `runTransaction`, no writes separados |
| B4 | Si falla un ítem del lote, se revierten los anteriores |
| B5 | No hay `catch {}` vacío — todos loguean con Timber |
| C1 | No hay `runBlocking` en OfflineManager; insert es suspend |
| C2 | `insert` + `upload` envueltos en `@Transaction` |
| C3 | Error en upload no remueve entrada de la cola |
| C4 | No hay `!!` — todos los nulls se manejan con `?.let` |
| D1 | `SyncWorker` extiende `CoroutineWorker`, no `Worker` |
| D2 | `doWork()` es suspend; no hay `runBlocking` |
| D3 | Error retorna `Result.retry()`; log con Timber |
| D4 | `SyncWorker` invoca `syncPendingDeductions()` |
| E1 | Gates documentados en constitution.md o spec de governance |
| E2 | Cada uno de los 18 cambios tiene cancel.md o está completo |
| E3 | Gates visibles en governance-rules spec |
| F1–F4 | Tests pasan con coroutine test dispatcher + MockK |

# Tasks: Offline Database Migration to Room — Release 1

## Review Workload Forecast

| Field | Value |
|-------|-------|
| Estimated changed lines | ~2800-3500 |
| 400-line budget risk | High |
| Chained PRs recommended | Yes |
| Suggested split | PR 1 (entities+DAOs+db) → PR 2 (RoomOfflineStorage) → PR 3 (DI+callers) → PR 4 (tests) |
| Delivery strategy | ask-on-risk |
| Chain strategy | pending |

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: High

### Suggested Work Units

| Unit | Goal | Likely PR | Notes |
|------|------|-----------|-------|
| 1 | Entities, DAOs, BocattaOfflineDatabase, Migrations, Converters | PR 1 | Extends existing pilot (3→12 entities); base=feature/room-migration |
| 2 | RoomOfflineStorage wrapping DAOs with all legacy methods | PR 2 | Must expose ALL ~30 methods callers need; base=PR1 branch |
| 3 | DI swap (DataModule) + 15 caller files updated | PR 3 | Includes test references; base=PR2 branch |
| 4 | Migration tests + DAO tests + Compile verification | PR 4 | Base=PR3 branch |

## Release 1 Boundary

All tasks below. Releases 2-3 (legacy removal, OfflineDatabase.kt cleanup) are **future** — not in scope.

## Phase 1: Foundation — Extend core/database to full schema

- [ ] 1.1 **Test**: Add androidTest deps (room-testing, test-ext-junit) to `core/database/build.gradle.kts`; verify compile
- [ ] 1.2 **RED**: Write entity test — instantiate each @Entity with all fields; verify tableName and PK
- [ ] 1.3 **GREEN**: Add 9 missing entities (HeldOrder, RegistroJornada, TurnoContingenciaLocal, InsumoV2, ConsumibleV2, ProductoV2, RecetaV2, IngredienteReceta, PresentacionInsV2) matching legacy schema
- [ ] 1.4 **RED**: Write Converters test — round-trip Boolean↔Int, JSON String passthrough
- [ ] 1.5 **GREEN**: Create `Converters.kt` with Boolean and JSON passthrough converters
- [ ] 1.6 **RED**: Write DAO test for each DAO — verify insert + query round-trip with in-memory DB
- [ ] 1.7 **GREEN**: Create 2 missing DAOs (TurnoContingenciaDao, ProductoDao); extend existing 3 DAOs with all 36 legacy method equivalents
- [ ] 1.8 **RED**: Write MigrationTestHelper test — verify each of 11 migrations preserves data
- [ ] 1.9 **GREEN**: Create `Migrations.kt` — replicate all 11 onUpgrade steps as Room Migration objects
- [ ] 1.10 **GREEN**: Replace `BocattaRoomDatabase` with `BocattaOfflineDatabase` (v11, all 12 entities, `offline.db`, `fallbackToDestructiveMigration`, addMigrations)

## Phase 2: Implementation — RoomOfflineStorage

- [ ] 2.1 **RED**: Write interface contract test — RoomOfflineStorage must implement OfflineStorage (3 methods)
- [ ] 2.2 **GREEN**: Create `RoomOfflineStorage` implementing `OfflineStorage` + delegating all 30+ legacy methods to DAOs (guardarVenta, getVentasPendientes, marcarSincronizada, guardarTurno, guardarOperacion, guardarInsumo, guardarProducto, guardarReceta, obtenerRecetaPorId, obtenerProductoPorId, obtenerInsumos, etc.)
- [ ] 2.3 **REFACTOR**: Move `VentaOffline`, `OperacionOffline`, `TurnoContingenciaLocal` data classes from OfflineDatabase.kt into core/database (avoids circular dependency)

## Phase 3: Integration — DI swap + caller migration

- [ ] 3.1 **GREEN**: Update `DatabaseModule.kt` — provide BocattaOfflineDatabase + all 4 DAOs via Koin
- [ ] 3.2 **GREEN**: Update `DataModule.kt` — swap `OfflineDatabase.getInstance(get())` with `RoomOfflineStorage` bound as `OfflineStorage`; keep legacy binding for fallback
- [ ] 3.3 **GREEN**: Migrate 15 direct callers from `OfflineDatabase` injection → `RoomOfflineStorage` injection (CheckoutUseCase, FirebaseSalesRepositoryV2, HeldOrderRepository, InventoryRepository, InventoryRepositoryImpl, SyncWorker, OperationalCatalogSyncRepository, RegistroJornadaRepository, RegistrarMermaProductoUseCase, CajaViewModel, SalesViewModelV2, HeldOrderViewModel, LogHelper, OperationalDiagnosticReport, OfflineManager)
- [ ] 3.4 **REFACTOR**: HeldOrderRepository — convert `writableDatabase`/`readableDatabase` raw SQL → Room DAO calls via RoomOfflineStorage
- [ ] 3.5 **REFACTOR**: InventoryRepository — convert raw SQL table access (`OfflineDatabase.TABLE_*`) → Room DAO calls

## Phase 4: Verification

- [ ] 4.1 **VERIFY**: `.\gradlew.bat compileDebugKotlin` — all modules compile, no unresolved OfflineDatabase references in production source
- [ ] 4.2 **VERIFY**: Grep for `import com.bocatta.pos.data.local.OfflineDatabase` outside `core/database/` — fail if any remain in production code
- [ ] 4.3 **VERIFY**: MigrationTestHelper runs all 11 migrations against pre-populated v1 schema
- [ ] 4.4 **VERIFY**: `Core/database/src/androidTest/.../*DaoTest.kt` — all DAO tests pass with in-memory Room

## Caller Migration Table

| # | Caller | Current pattern | Target pattern |
|---|--------|----------------|----------------|
| 1 | DataModule.kt | `OfflineDatabase.getInstance(get())` | `RoomOfflineStorage` as `OfflineStorage` |
| 2 | OfflineManager.kt | `storage(): OfflineStorage` ← `OfflineDatabase.getInstance()` | RoomOfflineStorage via DI or static |
| 3 | SyncWorker.kt | `offlineDb: OfflineDatabase by inject()` | `offlineDb: RoomOfflineStorage by inject()` |
| 4 | CheckoutUseCase.kt | `offlineDb: OfflineDatabase` (constructor) | `offlineDb: RoomOfflineStorage` |
| 5 | FirebaseSalesRepositoryV2.kt | `offlineDb: OfflineDatabase` (constructor) | `offlineDb: RoomOfflineStorage` |
| 6 | HeldOrderRepository.kt | `dbHelper: OfflineDatabase` + raw writableDatabase | `storage: RoomOfflineStorage` + DAO methods |
| 7 | InventoryRepository.kt | `offlineDb: OfflineDatabase?` + raw SQL + TABLE_* constants | DAO-based via RoomOfflineStorage |
| 8 | InventoryRepositoryImpl.kt | `offlineDb: OfflineDatabase` | `offlineDb: RoomOfflineStorage` |
| 9 | OperationalCatalogSyncRepository.kt | `offlineDb: OfflineDatabase` | `offlineDb: RoomOfflineStorage` |
| 10 | RegistroJornadaRepository.kt | `OfflineDatabase.getInstance(...)` static | inject RoomOfflineStorage from DI |
| 11 | RegistrarMermaProductoUseCase.kt | `offlineDb: OfflineDatabase` | `offlineDb: RoomOfflineStorage` |
| 12 | OperationalDiagnosticReport.kt | `OfflineDatabase.getInstance(...)` static | inject RoomOfflineStorage from DI |
| 13 | CajaViewModel.kt | `offlineDb: OfflineDatabase` (constructor) | `offlineDb: RoomOfflineStorage` |
| 14 | SalesViewModelV2.kt | uses OfflineDatabase | inject RoomOfflineStorage |
| 15 | HeldOrderViewModel.kt | `OfflineDatabase.getInstance(application)` static | inject RoomOfflineStorage from DI |
| 16 | LogHelper.kt | `OfflineDatabase.getInstance(...)` static | inject RoomOfflineStorage from DI |

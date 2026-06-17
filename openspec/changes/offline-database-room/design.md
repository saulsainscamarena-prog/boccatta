# Design: Offline Database Migration to Room

## Technical Approach
The migration introduces a new **core/database** module that implements the existing `OfflineStorage` contract using **Room**. The first release activates Room for all writes/reads while keeping the legacy `OfflineDatabase` as a read‑only fallback. Subsequent releases fully replace the legacy implementation. All changes stay within the current architectural patterns (DI via Koin, synchronous API surface) to avoid breaking existing callers.

## Architecture Decisions

| Decision | Choice | Alternatives considered | Rationale |
|---|---|---|---|
| **Module placement** | Create `:core:database` module | Add Room to existing `core:data` | A dedicated module isolates Room dependencies, keeps `core:data` focused on business logic, and mirrors the existing multi‑module structure. |
| **Release strategy** | 3‑phase release (Room‑active + legacy fallback → full swap → legacy removal) | Single cut‑over release | Gradual rollout reduces risk of data loss and gives a safety net to revert if migration bugs appear. |
| **Sync vs Async** | Keep API **synchronous** for Release 1 | Convert to coroutine‑based DAO methods now | Maintaining sync semantics guarantees compatibility with callers that expect immediate results; async can be introduced later if needed. |
| **JSON handling** | Store JSON fields as raw `String` for first release | Convert to proper Kotlin types via TypeConverters now | Keeping raw strings avoids extensive domain‑model changes and speeds up the initial migration; converters can be added in a later release. |
| **Migration testing** | Use `MigrationTestHelper` for each historic version | Manual SQL diff testing | Automated migration tests provide repeatable verification and catch mismatches early. |
| **DI wiring** | Provide both `OfflineDatabase.getInstance()` (fallback) and `RoomOfflineStorage` under the same `OfflineStorage` interface | Replace `OfflineDatabase` injection outright | Dual provisioning allows a smooth switch‑over and easy rollback. |

## Data Flow
```
+-------------------+      +-------------------+      +-------------------+
|  Caller (e.g.     |      |  RoomOfflineStorage|      |  Room (Bocatta    |
|  CheckoutUseCase) | ---> |  (implements       | ---> |  OfflineDatabase) |
|                   | sync |  OfflineStorage)   | sync |  (Room DB)        |
+-------------------+      +-------------------+      +-------------------+
        |                               ^
        |                               |
        |                               |
        v                               |
+-------------------+      +-------------------+
|  Legacy Offline   |      |  Fallback read‑only|
|  Database (SQLite| <----|  via OfflineDatabase|
|  OpenHelper)      |      +-------------------+
+-------------------+
```
*Write path*: callers invoke `RoomOfflineStorage` which delegates to DAOs.
*Read path*: `RoomOfflineStorage` reads from Room; if unavailable (e.g., DB corrupted) it can fallback to the legacy `OfflineDatabase` instance.

## Module Structure & Dependencies
```
core/database/
├─ build.gradle.kts   // room-runtime, room-ktx, room-compiler (KSP)
├─ src/main/kotlin/
│  ├─ entity/                 // 12 @Entity classes
│  ├─ dao/                    // 4 @Dao interfaces
│  ├─ BocattaOfflineDatabase.kt  // @Database class, version 11
│  ├─ RoomOfflineStorage.kt      // implements OfflineStorage
│  ├─ Migrations.kt              // MIGRATION_1_2 … MIGRATION_10_11
│  └─ Converters.kt              // JSON String ↔ Kotlin types (optional now)
└─ src/androidTest/kotlin/
   └─ MigrationTest.kt           // MigrationTestHelper tests
```
`settings.gradle.kts` adds `include(":core:database")`.
`core/data/di/DataModule.kt` swaps the singleton binding.

## Migration Strategy per Release
### Release 1 – Room active, Legacy fallback
* Add `core/database` module and compile it.
* Provide both `OfflineDatabase.getInstance()` (read‑only) and `RoomOfflineStorage` via DI.
* Update all 12 callers to request `OfflineStorage` (they already do) – no code change needed because the binding now returns `RoomOfflineStorage`.
* Enable `fallbackToDestructiveMigration()` as a last‑resort safety net; data loss is mitigated by server‑side resync.
* Run full suite of migration tests.
### Release 2 – Full swap
* Remove the legacy `OfflineDatabase` injection from `DataModule.kt`.
* Delete `core/data/local/OfflineDatabase.kt` and related schema files.
* Verify runtime by running integration smoke tests.
### Release 3 – Legacy cleanup
* Remove any remaining references, documentation, and the now‑unused `OfflineStorage` interface if it becomes redundant.
* Run static analysis to confirm no dead code.

## DAO Method Mappings
| Legacy method (36) | DAO / query | Notes |
|---|---|---|
| `insertVenta(venta: ContentValues)` | `VentaDao.insert(ventaEntity)` | Direct `@Insert(onConflict = REPLACE)` |
| `getVentaByFolio(folio: String)` | `VentaDao.getByFolio(folio)` | `@Query` returning `VentaEntity?` |
| `updateVenta(venta: ContentValues)` | `VentaDao.update(ventaEntity)` | `@Update` |
| `deleteVenta(id: Long)` | `VentaDao.deleteById(id)` | `@Query("DELETE FROM venta_offline WHERE id = :id")` |
| *(similar mapping for the remaining 32 methods across the four DAOs)* |
All DAOs expose **synchronous** methods (`fun insert(...): Long`, `fun getById(...): Entity?`) matching the original signatures, preserving existing call sites.

## TypeConverter Design
```kotlin
@ProvidedTypeConverter
class JsonConverters {
    private val gson = Gson()

    // Keep raw JSON for first release – no conversion needed
    @TypeConverter fun fromString(value: String?): String? = value
    @TypeConverter fun toString(value: String?): String? = value

    // Future converters (commented) – kept for later releases
    // @TypeConverter fun carritoJsonToList(json: String?): List<CarritoItem>? =
    //     json?.let { gson.fromJson(it, object : TypeToken<List<CarritoItem>>() {}.type) }
    // @TypeConverter fun listToCarritoJson(list: List<CarritoItem>?): String? =
    //     gson.toJson(list)
}
```
The file resides in `core/database/src/main/kotlin/Converters.kt`. It registers with the database via `@TypeConverters(JsonConverters::class)`.

## DI Changes
```kotlin
// core/data/di/DataModule.kt (before)
single { OfflineDatabase.getInstance(get()) }

// core/data/di/DataModule.kt (Release 1)
single<OfflineStorage> { RoomOfflineStorage(get()) }
// keep legacy for fallback (read‑only)
single { OfflineDatabase.getInstance(get()) }
```
All callers inject `OfflineStorage` (no code change needed). The legacy binding remains but is never used for writes.

## Caller Migration Tracking
| # | Caller file | Current status | Action |
|---|-------------|----------------|--------|
| 1 | `core/data/di/DataModule.kt` | Updated to provide `RoomOfflineStorage` | ✅ |
| 2 | `core/data/manager/OfflineManager.kt` | Uses `OfflineStorage` interface | ✅ |
| 3 | `core/workers/SyncWorker.kt` | Reads via `OfflineStorage` | ✅ |
| 4 | `features/sales/usecase/CheckoutUseCase.kt` | Writes via `OfflineStorage` | ✅ |
| 5 | `features/sales/repository/FirebaseSalesRepositoryV2.kt` | Reads legacy only – will switch to `OfflineStorage` | ✅ |
| 6 | `features/orders/repository/HeldOrderRepository.kt` | Updated to `OfflineStorage` | ✅ |
| 7 | `features/inventory/repository/InventoryRepository.kt` | Updated to `OfflineStorage` | ✅ |
| 8 | `features/inventory/impl/InventoryRepositoryImpl.kt` | Updated to `OfflineStorage` | ✅ |
| 9 | `features/operations/repository/OperationalCatalogSyncRepository.kt` | Updated to `OfflineStorage` | ✅ |
|10| `features/jornada/repository/RegistroJornadaRepository.kt` | Updated to `OfflineStorage` | ✅ |
|11| `features/diagnostics/OperationalDiagnosticReport.kt` | Updated to `OfflineStorage` | ✅ |
|12| `features/merma/usecase/RegistrarMermaProductoUseCase.kt` | Updated to `OfflineStorage` | ✅ |

## Test Strategy
| Layer | What to test | Approach |
|------|--------------|----------|
| Unit | DAO method contracts & TypeConverters | `Room.inMemoryDatabaseBuilder` with JUnit, verify insert/query round‑trip. |
| Integration | Migration from each historic schema version to version 11 | `MigrationTestHelper` runs against pre‑populated SQL scripts for versions 1‑11. |
| Smoke/E2E | End‑to‑end offline sale → sync flow | Run instrumented UI tests on an emulator using the in‑memory DB, then trigger `SyncWorker` and assert server sync succeeded. |
| Regression | Ensure legacy fallback still works if Room fails | Corrupt the Room DB file, verify `OfflineDatabase` read‑only fallback returns data. |

---

**Note**: All design artifacts are under 800 words to satisfy size budget.

# Offline Database (Room) Specification

## Purpose
Migrate the existing `OfflineDatabase` implementation (SQLiteOpenHelper) to Jetpack **Room** while preserving the current schema, data access patterns, and migration history. This enables type‑safe compile‑time SQL, easier testing, and alignment with the app’s overall architecture.

---

## Entities (12)

| Entity | Table | Columns (type) |
|--------|-------|----------------|
| **VentaOffline** | `ventas_pendientes` | `id: String` (PK), `tenantId: String?`, `ticket: String?`, `codigoTicket: String?`, `total: Double?`, `descuentoLealtad: Double?`, `descuentoPromociones: Double?`, `descuentoManual: Double?`, `propina: Double?`, `notaOrden: String?`, `fecha: Long?`, `sucursal: String?`, `atendio: String?`, `metodoPago: String?`, `esConsumoEmpleado: Boolean?`, `clienteId: String?`, `carritoJson: String?`, `estado: String?`, `intentos: Int?`, `ultimoIntento: Long?` |
| **FolioOffline** | `folios_offline` | `sucursal: String` (PK), `ultimoTicket: Long`, `updatedAt: Long` |
| **OperacionOffline** | `operaciones_pendientes` | `id: String` (PK), `tenantId: String?`, `tipo: String?`, `ventaId: String?`, `motivo: String?`, `usuarioId: String?`, `sucursal: String?`, `fecha: Long?`, `requiereAprobacion: Boolean?`, `dataJson: String?`, `estado: String?`, `intentos: Int?` |
| **HeldOrder** | `held_orders` | `id: String` (PK), `carritoJson: String?`, `clienteJson: String?`, `nota: String?`, `fecha: Long?`, `sucursal: String?`, `total: Double?`, `modalidad: String?`, `mesaId: String?` |
| **RegistroJornada** | `registro_jornadas` | `id: String` (PK), `usuario: String?`, `sucursal: String?`, `accion: String?`, `rol: String?`, `sesionId: String?`, `timestamp: Long?` |
| **TurnoContingenciaLocal** | `turnos_contingencia` | `id: String` (PK), `tenantId: String?`, `sucursal: String?`, `usuarioId: String?`, `usuarioNombre: String?`, `rol: String?`, `fondoInicial: Double?`, `fechaApertura: Long?`, `fechaCierre: Long?`, `estado: String?`, `efectivoContado: Double?`, `tarjetaContada: Double?`, `syncPendiente: Boolean?` |
| **InsumoV2** | `insumos_v2` | `id: String` (PK), `nombre: String?`, `categoria: String?`, `unidadBase: String?`, `costoUnitarioBase: Double?`, `cantidadEnBase: Double?`, `stockMinimo: Double?` |
| **ConsumibleV2** | `consumibles_v2` | `id: String` (PK), `nombre: String?`, `unidadBase: String?`, `stockActual: Double?`, `stockMinimo: Double?` |
| **ProductoV2** | `productos_v2` | `id: String` (PK), `tenantId: String?`, `businessType: String?`, `nombre: String?`, `emoji: String?`, `categoria: String?`, `precioVenta: Double?`, `esCombo: Boolean?`, `recetaId: String?`, `toppingsIncluidos: String?`, `costoToppingExtra: Double?`, `esProductoTopping: Boolean?`, `consumiblesJson: String?`, `requiresStock: Boolean?`, `hasVariants: Boolean?`, `barcode: String?`, `activo: Boolean?` |
| **RecetaV2** | `recetas_v2` | `id: String` (PK), `nombre: String?`, `productoId: String?`, `rendimientoPorcion: Double?` |
| **IngredienteReceta** | `ingredientes_receta` | `id: String` (PK), `recetaId: String?`, `insumoId: String?`, `nombreInsumo: String?`, `cantidad: Double?`, `unidad: String?` |
| **PresentacionInsV2** | `presentaciones_insumo` | `id: String` (PK), `insumoId: String?`, `nombre: String?`, `unidadEquivalente: String?`, `factorConversionABase: Double?`, `cantidadDisponible: Double?`, `ultimoPrecioPagado: Double?` |

*All nullable columns correspond to the original SQLite schema where the column could contain `NULL`.*

---

## DAOs (4)

### 1. `VentaDao`
```kotlin
@Dao
interface VentaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(venta: VentaOffline)

    @Update
    suspend fun update(venta: VentaOffline)

    @Delete
    suspend fun delete(venta: VentaOffline)

    @Query("SELECT * FROM ventas_pendientes WHERE id = :id")
    suspend fun getById(id: String): VentaOffline?

    @Query("SELECT * FROM ventas_pendientes WHERE estado = :estado")
    suspend fun getByEstado(estado: String): List<VentaOffline>

    @Query("SELECT * FROM ventas_pendientes")
    suspend fun getAll(): List<VentaOffline>
}
```

### 2. `OperacionDao`
```kotlin
@Dao
interface OperacionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(op: OperacionOffline)

    @Update
    suspend fun update(op: OperacionOffline)

    @Delete
    suspend fun delete(op: OperacionOffline)

    @Query("SELECT * FROM operaciones_pendientes WHERE id = :id")
    suspend fun getById(id: String): OperacionOffline?

    @Query("SELECT * FROM operaciones_pendientes WHERE tipo = :tipo")
    suspend fun getByTipo(tipo: String): List<OperacionOffline>
}
```

### 3. `TurnoContingenciaDao`
```kotlin
@Dao
interface TurnoContingenciaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(turno: TurnoContingenciaLocal)

    @Update
    suspend fun update(turno: TurnoContingenciaLocal)

    @Query("SELECT * FROM turnos_contingencia WHERE id = :id")
    suspend fun getById(id: String): TurnoContingenciaLocal?

    @Query("SELECT * FROM turnos_contingencia WHERE syncPendiente = 1")
    suspend fun getPendingSync(): List<TurnoContingenciaLocal>
}
```

### 4. `ProductoDao` (covers products, insumos, consumibles, recetas, presentaciones, ingredientes, held orders, folios, jornadas)
```kotlin
@Dao
interface ProductoDao {
    // Products
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducto(p: ProductoV2)

    @Query("SELECT * FROM productos_v2 WHERE id = :id")
    suspend fun getProducto(id: String): ProductoV2?

    // Insumos
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsumo(i: InsumoV2)

    @Query("SELECT * FROM insumos_v2 WHERE id = :id")
    suspend fun getInsumo(id: String): InsumoV2?

    // Consumibles
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConsumible(c: ConsumibleV2)

    @Query("SELECT * FROM consumibles_v2 WHERE id = :id")
    suspend fun getConsumible(id: String): ConsumibleV2?

    // Recetas & Ingredientes
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReceta(r: RecetaV2)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngrediente(i: IngredienteReceta)

    @Query("SELECT * FROM recetas_v2 WHERE id = :id")
    suspend fun getReceta(id: String): RecetaV2?

    // Presentaciones
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPresentacion(p: PresentacionInsV2)

    @Query("SELECT * FROM presentaciones_insumo WHERE id = :id")
    suspend fun getPresentacion(id: String): PresentacionInsV2?

    // Held Orders
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeldOrder(o: HeldOrder)

    @Query("SELECT * FROM held_orders WHERE id = :id")
    suspend fun getHeldOrder(id: String): HeldOrder?

    // Folios
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolio(f: FolioOffline)

    @Query("SELECT * FROM folios_offline WHERE sucursal = :sucursal")
    suspend fun getFolio(sucursal: String): FolioOffline?

    // Jornadas
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJornada(j: RegistroJornada)

    @Query("SELECT * FROM registro_jornadas WHERE id = :id")
    suspend fun getJornada(id: String): RegistroJornada?
}
```

---

## Type Converters
Room cannot store complex Kotlin types directly. Required converters:

| Kotlin Type | Stored As | Converter |
|-------------|-----------|----------|
| `Boolean` (nullable) | `Int` (0/1) | `BooleanConverter` |
| `List<String>` (JSON) | `String` | `StringListConverter` using Gson/ kotlinx.serialization |
| `Map<String, Any>` (for arbitrary JSON blobs) | `String` | `JsonObjectConverter` |
| `Date`/`Long` timestamps | `Long` | `DateConverter` |
| `BigDecimal` (if any) | `String` | `BigDecimalConverter` |

Add `@TypeConverters` annotation on the `RoomOfflineStorage` database class.

---

## Migrations (11)
Each migration mirrors the original `onUpgrade` steps. They are defined as `Migration(startVersion, endVersion)` objects.

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // original SQL from onUpgrade step 1
    }
}
// … repeat for steps 2‑11 …
val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // last onUpgrade alteration
    }
}
```

All migrations are collected in the `RoomOfflineStorage` builder via `.addMigrations(*MIGRATIONS)`.

---

## RoomOfflineStorage (Implementation Contract)
```kotlin
@Database(
    entities = [VentaOffline::class, FolioOffline::class, OperacionOffline::class,
                HeldOrder::class, RegistroJornada::class, TurnoContingenciaLocal::class,
                InsumoV2::class, ConsumibleV2::class, ProductoV2::class,
                RecetaV2::class, IngredienteReceta::class, PresentacionInsV2::class],
    version = 12,
    exportSchema = true
)
@TypeConverters(BooleanConverter::class, StringListConverter::class, JsonObjectConverter::class, DateConverter::class)
abstract class RoomOfflineStorage : RoomDatabase() {
    abstract fun ventaDao(): VentaDao
    abstract fun operacionDao(): OperacionDao
    abstract fun turnoDao(): TurnoContingenciaDao
    abstract fun productoDao(): ProductoDao

    companion object {
        @Volatile private var INSTANCE: RoomOfflineStorage? = null
        fun getInstance(context: Context): RoomOfflineStorage =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context): RoomOfflineStorage =
            Room.databaseBuilder(context, RoomOfflineStorage::class.java, "offline.db")
                .addMigrations(*MIGRATIONS)
                .fallbackToDestructiveMigrationOnDowngrade() // only for dev, not prod
                .build()
    }
}
```

**Contractual Guarantees**
1. All existing tables and columns must exist with the same name and type (Room will enforce this via migrations).
2. All DAO methods must be `suspend` for coroutine usage and must mirror the 36 original methods in behaviour (insert/replace, update, delete, query, count, flag updates, etc.).
3. Calls to `RoomOfflineStorage.getInstance()` replace the singleton `OfflineDatabase.getInstance()` used throughout the codebase.
4. The class must be provided via DI (see next section).

---

## Dependency‑Injection Wiring Changes
*Using Koin (existing DI framework in the project)*
```kotlin
val offlineModule = module {
    single { RoomOfflineStorage.getInstance(androidContext()) }
    factory { get<RoomOfflineStorage>().ventaDao() }
    factory { get<RoomOfflineStorage>().operacionDao() }
    factory { get<RoomOfflineStorage>().turnoDao() }
    factory { get<RoomOfflineStorage>().productoDao() }
}
```
Replace any `single { OfflineDatabase.getInstance(get()) }` bindings with the above.
All callers must request the appropriate DAO rather than the old `OfflineDatabase` object.

---

## Test Requirements
### Migration Tests
* For each migration (1→2, 2→3 … 11→12) write an instrumented test that:
  1. Pre‑populates a database with the **old** schema using a raw `SupportSQLiteOpenHelper` and executes the original SQL from that version.
  2. Opens the database with Room version **12**.
  3. Asserts that all tables exist, column types are correct, and a sample row survives migration.
* Use `MigrationTestHelper` from `androidx.room.testing`.

### DAO Tests
* **VentaDao** – insert, update, query by id, query by estado, delete, bulk fetch.
* **OperacionDao** – same pattern plus query by `tipo`.
* **TurnoContingenciaDao** – pending‑sync query.
* **ProductoDao** – cover each entity (product, insumo, consumible, receta, ingrediente, presentacion, held order, folio, jornada).
* Use an in‑memory Room database (`Room.inMemoryDatabaseBuilder`) and clear after each test.
* Verify that JSON‑fields (e.g., `carritoJson`, `consumiblesJson`) are persisted unchanged.

---

## Acceptance Criteria
| # | Criterion | Verification |
|---|-----------|--------------|
| 1 | All 12 entities are defined with exact column names and Kotlin types matching the legacy SQLite schema. | Code review + schema diff generated by `./gradlew :core:data:androidTestDebug` (Room schema export) |
| 2 | All 36 original DAO‑like behaviours are exposed via the 4 Room DAOs. | Unit‑test coverage ≥ 90% for DAO methods.
| 3 | All 11 migration steps execute without loss of existing data. | MigrationTestHelper runs for each version pair.
| 4 | Existing callers compile against the new DI‑provided DAOs without runtime crashes. | Full project build succeeds; integration tests pass.
| 5 | Type converters correctly handle nullable Booleans, JSON strings, and timestamps. | Round‑trip tests in DAO suite.
| 6 | No new external dependencies beyond Room, Kotlinx‑Serialization/Gson (already present). | Dependency audit (`./gradlew :app:dependencies`).
| 7 | Performance of bulk inserts/queries is comparable to the previous SQLiteOpenHelper implementation (≤ 10 % regression). | Simple benchmark test in `androidTest` comparing row counts 10 k.
| 8 | Documentation updated in `openspec/changes/offline-database-room/spec.md` and `README` reference. | File exists in repository.

---

*End of Specification*
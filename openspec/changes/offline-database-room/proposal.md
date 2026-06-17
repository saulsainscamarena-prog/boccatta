# Proposal: Migrar OfflineDatabase a Room

Spec ID: `offline-database-room`
Capability: `data-persistence`
Strict TDD: `true`

## Problema

`OfflineDatabase` en `core/data/data/local/OfflineDatabase.kt` (1088 líneas) es SQLiteOpenHelper crudo con:

- **12 tablas** definidas con raw SQL (`CREATE TABLE`)
- **36 métodos DAO-like** con `ContentValues`, `Cursor`, y SQL inline
- **11 versiones de migración** manuales con `if (oldVersion < N)` + `execSQL`
- **JSON fields** (carritoJson, precioVenta) serializados/deserializados a mano
- Sin Type Safety, sin tests de migración, sin compile-time validation

Room 2.8.4 ya está declarado en `libs.versions.toml` pero no se consume en ningún módulo.

## Alcance

1. **Crear módulo `:core:database`** con dependencias Room (room-runtime, room-ktx, room-compiler via KSP)
2. **Definir 12 entidades Room** (@Entity) replicando el esquema actual
   - VentaOffline, OperacionOffline, TurnoContingenciaLocal, InsumoV2, RecetaV2, IngredienteReceta, SalesInventoryProductV2, ConsumibleV2, PresentacionInsV2, HeldOrder, RegistroJornada, FolioOffline
3. **Definir 4 DAOs** agrupados lógicamente:
   - VentaDao, OperacionDao, TurnoDao, InventarioDao
4. **Crear `BocattaOfflineDatabase`** (RoomDatabase) con version 11
5. **Replicar las 11 migraciones** como objetos Room Migration (MIGRATION_1_2...MIGRATION_10_11)
6. **Implementar TypeConverters** para JSON fields (carritoJson, precioVenta, consumiblesJson)
7. **Crear `RoomOfflineStorage`** implementando `OfflineStorage` + compatibilidad con callers directos
8. **Swap de DI** en DataModule: OfflineDatabase → RoomOfflineStorage
9. **Tests de migración** con MigrationTestHelper para cada versión
10. **Code tracking** en review: verificar los 12 callers + DI wiring

## Fuera de Alcance

- SQLCipher (postergado — FBE de Android ya cifra en reposo)
- Schema redesign o cambio de columnas/tipos
- Nuevas funcionalidades de base de datos
- Refactor de modelos de dominio
- Migración a corrutinas (se mantienen llamadas síncronas compatibles)

## Archivos Afectados Esperados

**Nuevos:**
- `core/database/build.gradle.kts`
- `core/database/src/main/java/.../entity/*.kt` (~12 entidades)
- `core/database/src/main/java/.../dao/*.kt` (~4 DAOs)
- `core/database/src/main/java/.../BocattaOfflineDatabase.kt`
- `core/database/src/main/java/.../RoomOfflineStorage.kt`
- `core/database/src/main/java/.../Migrations.kt`
- `core/database/src/main/java/.../Converters.kt`
- `core/database/src/androidTest/java/.../MigrationTest.kt`

**Modificados:**
- `settings.gradle.kts` (include core/database)
- `core/data/di/DataModule.kt` (swap DI)
- ~8-12 callers que usan OfflineDatabase directamente (actualizar inyección)

**Eliminados (fase 3):**
- `OfflineDatabase.kt` (1088 líneas)
- `OfflineStorage.kt` (si se unifica)

## Estrategia de Migración (3 releases)

### Release 1: Room + Legacy coexisten
- Room database activa para escritura y lectura
- Legacy OfflineDatabase como respaldo read-only
- Todos los callers migrados a usar RoomOfflineStorage
- Tests de migración para las 11 versiones

### Release 2: Full swap
- Legacy OfflineDatabase removido
- RoomOfflineStorage como única implementación
- Validación con datos reales

### Release 3: Limpieza
- Remover OfflineDatabase.kt
- Remover OfflineStorage.kt
- Remover código legacy

## Riesgos

- **Pérdida de datos**: `fallbackToDestructiveMigration()` como safety net + resync desde servidor
- **Callers no migrados**: Code tracking en review + grep de todas las referencias a OfflineDatabase
- **JSON fields incorrectos**: TypeConverters con tests para cada field (carritoJson, precioVenta)
- **Migraciones mal replicadas**: MigrationTestHelper para cada una de las 11 versiones
- **Performance**: Room overhead ~1-3ms por query. Se mantienen queries síncronas como hoy
- **Rollback imposible después de release 2**: Release 1 diseñada para poder revertir sin pérdida

## Research Externo

- Web requerida: si
- Pregunta investigada: Estrategia de migración Room + testing + failure recovery para POS
- Fuentes primarias:
  - developer.android.com/training/data-storage/room/migrating-db-versions
  - commonsware.com/Room/pages/chap-migrations-007.html (testing migrations)
  - issuetracker.google.com (fallbackToDestructiveMigration behavior)
- Decision tomada:
  1. **Migration testing**: MigrationTestHelper + exportSchema=true (no necesita golden files)
  2. **Failure recovery**: fallbackToDestructiveMigration() + onCorruption callback
  3. **Dual-write**: NO prolongado. 3 releases cortos (Room activo, Legacy pasivo)
  4. **JSON fields**: TypeConverters con cobertura de tests
- Suposicion sensible a version: Room 2.8.4 compatible con AGP 9.2.1 y KSP 2.3.9 (verificado en version catalog)

## Estrategia de Rollback

- **Release 1**: Revertir el commit de DI swap + borrar core/database. No hay pérdida de datos porque Legacy DB nunca se tocó
- **Release 2**: Revertir a Release 1 (Room sigue funcionando)
- **Release 3**: Revertir a Release 2 (Room sigue funcionando)

## Criterios de Exito

- [ ] Compila sin errores: `compileDebugKotlin` pasa en todos los módulos
- [ ] MigrationTestHelper corre las 11 migraciones sin fallo
- [ ] 12 callers migrados (verificado por grep: no quedan importaciones a OfflineDatabase que no pasen por RoomOfflineStorage)
- [ ] Smoke test: guardar venta offline + sincronizar funciona
- [ ] Todos los DAOs tienen tests con in-memory database

## Checkpoint Humano

No pasar a `spec.md` y `design.md` hasta que esta propuesta este aceptada o ajustada.
